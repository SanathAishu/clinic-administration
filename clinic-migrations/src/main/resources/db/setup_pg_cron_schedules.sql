-- =====================================================================================
-- pg_cron Schedule Setup for Materialized Views
-- =====================================================================================
--
-- PREREQUISITES:
-- 1. Install pg_cron extension (requires superuser):
--    psql -U postgres -d clinic -c "CREATE EXTENSION IF NOT EXISTS pg_cron;"
--
-- 2. Ensure pg_cron is properly configured in postgresql.conf:
--    shared_preload_libraries = 'pg_cron'
--    cron.database_name = 'clinic'
--
-- 3. Restart PostgreSQL after configuration changes
--
-- EXECUTION:
-- Run this script as a database user with cron privileges:
--   psql -U clinic_user -d clinic -f setup_pg_cron_schedules.sql
--
-- Or via Docker:
--   docker exec -i clinic-postgres psql -U clinic_user -d clinic < setup_pg_cron_schedules.sql
-- =====================================================================================

-- Enable pg_cron extension (requires superuser, may already be enabled)
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- =====================================================================================
-- REMOVE EXISTING SCHEDULES (if re-running script)
-- =====================================================================================

-- List existing jobs (for reference)
SELECT jobid, schedule, command, jobname
FROM cron.job
WHERE jobname IN ('refresh-patient-summary', 'refresh-billing-summary', 'refresh-notification-summary');

-- Unschedule existing jobs (prevents duplicates)
SELECT cron.unschedule(jobid)
FROM cron.job
WHERE jobname IN ('refresh-patient-summary', 'refresh-billing-summary', 'refresh-notification-summary');

-- =====================================================================================
-- CREATE REFRESH SCHEDULES
-- =====================================================================================

-- Schedule 1: Patient Clinical Summary (Every 15 minutes)
-- Cron: */15 * * * * (at minute 0, 15, 30, 45 of every hour)
DO $$
DECLARE
    job_id BIGINT;
BEGIN
    SELECT cron.schedule(
        'refresh-patient-summary',
        '*/15 * * * *',
        'SELECT refresh_patient_clinical_summary()'
    ) INTO job_id;

    RAISE NOTICE 'Scheduled patient summary refresh (every 15 min) - Job ID: %', job_id;
END $$;

-- Schedule 2: Billing Summary (Every hour at minute 0)
-- Cron: 0 * * * * (at minute 0 of every hour)
DO $$
DECLARE
    job_id BIGINT;
BEGIN
    SELECT cron.schedule(
        'refresh-billing-summary',
        '0 * * * *',
        'SELECT refresh_billing_summary()'
    ) INTO job_id;

    RAISE NOTICE 'Scheduled billing summary refresh (hourly) - Job ID: %', job_id;
END $$;

-- Schedule 3: Notification Summary (Every 5 minutes)
-- Cron: */5 * * * * (at minute 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55 of every hour)
DO $$
DECLARE
    job_id BIGINT;
BEGIN
    SELECT cron.schedule(
        'refresh-notification-summary',
        '*/5 * * * *',
        'SELECT refresh_notification_summary()'
    ) INTO job_id;

    RAISE NOTICE 'Scheduled notification summary refresh (every 5 min) - Job ID: %', job_id;
END $$;

-- =====================================================================================
-- VERIFICATION
-- =====================================================================================

-- Display all scheduled jobs
SELECT
    jobid,
    schedule,
    command,
    nodename,
    nodeport,
    database,
    username,
    active,
    jobname
FROM cron.job
WHERE jobname IN ('refresh-patient-summary', 'refresh-billing-summary', 'refresh-notification-summary')
ORDER BY jobname;

-- Check recent job runs (if any)
SELECT
    jobid,
    runid,
    job_pid,
    database,
    username,
    command,
    status,
    return_message,
    start_time,
    end_time
FROM cron.job_run_details
WHERE jobid IN (
    SELECT jobid FROM cron.job
    WHERE jobname IN ('refresh-patient-summary', 'refresh-billing-summary', 'refresh-notification-summary')
)
ORDER BY start_time DESC
LIMIT 10;

-- =====================================================================================
-- MANUAL REFRESH COMMANDS (for testing)
-- =====================================================================================

-- Manually trigger refreshes to test:
-- SELECT refresh_patient_clinical_summary();
-- SELECT refresh_billing_summary();
-- SELECT refresh_notification_summary();

-- =====================================================================================
-- SCHEDULE MANAGEMENT COMMANDS (for future reference)
-- =====================================================================================

-- To temporarily disable a job:
-- SELECT cron.unschedule('refresh-patient-summary');

-- To re-enable a disabled job, re-run the schedule command above

-- To view job execution history:
-- SELECT * FROM cron.job_run_details
-- WHERE jobid = <job_id>
-- ORDER BY start_time DESC
-- LIMIT 20;

-- To alter a schedule (must unschedule first, then reschedule):
-- SELECT cron.unschedule('refresh-patient-summary');
-- SELECT cron.schedule('refresh-patient-summary', '*/10 * * * *', 'SELECT refresh_patient_clinical_summary()');

-- =====================================================================================
-- NOTES
-- =====================================================================================

-- Cron Schedule Format: minute hour day-of-month month day-of-week
-- Examples:
--   */5 * * * *     Every 5 minutes
--   */15 * * * *    Every 15 minutes
--   0 * * * *       Every hour (at minute 0)
--   0 */2 * * *     Every 2 hours
--   0 0 * * *       Daily at midnight
--   0 2 * * *       Daily at 2 AM
--   0 0 * * 0       Weekly on Sunday at midnight
--   0 0 1 * *       Monthly on the 1st at midnight

-- Performance Considerations:
-- - CONCURRENTLY option allows non-blocking refreshes
-- - Requires unique indexes on materialized views
-- - Schedules are staggered to avoid overlapping heavy refreshes
-- - Monitor cron.job_run_details for failures and performance issues

-- =====================================================================================
-- END OF SETUP SCRIPT
-- =====================================================================================

\echo '===================================================================================='
\echo 'pg_cron schedules configured successfully!'
\echo ''
\echo 'Active Schedules:'
\echo '  - Patient Clinical Summary: Every 15 minutes'
\echo '  - Billing Summary: Every hour'
\echo '  - Notification Summary: Every 5 minutes'
\echo ''
\echo 'To verify schedules: SELECT * FROM cron.job;'
\echo 'To view job history: SELECT * FROM cron.job_run_details ORDER BY start_time DESC;'
\echo '===================================================================================='
