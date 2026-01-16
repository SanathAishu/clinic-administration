# Materialized Views Refresh Alternatives

## Issue: pg_cron Not Available

The PostgreSQL container does not have the `pg_cron` extension installed. This extension would enable automated scheduled refreshes directly within the database.

**Error:**
```
ERROR:  extension "pg_cron" is not available
DETAIL:  Could not open extension control file
```

## Alternative Refresh Strategies

Since pg_cron is not available, here are the recommended alternatives for refreshing materialized views:

---

## Option 1: Spring Boot @Scheduled Tasks (RECOMMENDED)

Use Spring Boot's built-in scheduling to refresh materialized views from the application layer.

### Implementation:

**Step 1: Create Repository Interface**

```java
package com.clinic.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MaterializedViewRefreshRepository extends JpaRepository<Object, Long> {

    @Modifying
    @Transactional
    @Query(value = "SELECT refresh_patient_clinical_summary()", nativeQuery = true)
    void refreshPatientClinicalSummary();

    @Modifying
    @Transactional
    @Query(value = "SELECT refresh_billing_summary()", nativeQuery = true)
    void refreshBillingSummary();

    @Modifying
    @Transactional
    @Query(value = "SELECT refresh_notification_summary()", nativeQuery = true)
    void refreshNotificationSummary();
}
```

**Step 2: Create Scheduled Service**

```java
package com.clinic.backend.service;

import com.clinic.backend.repository.MaterializedViewRefreshRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterializedViewRefreshService {

    private final MaterializedViewRefreshRepository refreshRepository;

    /**
     * Refresh patient clinical summary every 15 minutes
     * Cron: 0 */15 * * * * (at second 0, every 15 minutes)
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void refreshPatientClinicalSummary() {
        try {
            log.info("Starting refresh of mv_patient_clinical_summary at {}", Instant.now());
            refreshRepository.refreshPatientClinicalSummary();
            log.info("Successfully refreshed mv_patient_clinical_summary");
        } catch (Exception e) {
            log.error("Failed to refresh mv_patient_clinical_summary", e);
        }
    }

    /**
     * Refresh billing summary every hour
     * Cron: 0 0 * * * * (at minute 0 of every hour)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshBillingSummary() {
        try {
            log.info("Starting refresh of mv_billing_summary_by_period at {}", Instant.now());
            refreshRepository.refreshBillingSummary();
            log.info("Successfully refreshed mv_billing_summary_by_period");
        } catch (Exception e) {
            log.error("Failed to refresh mv_billing_summary_by_period", e);
        }
    }

    /**
     * Refresh notification summary every 5 minutes
     * Cron: 0 */5 * * * * (at second 0, every 5 minutes)
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void refreshNotificationSummary() {
        try {
            log.info("Starting refresh of mv_user_notification_summary at {}", Instant.now());
            refreshRepository.refreshNotificationSummary();
            log.info("Successfully refreshed mv_user_notification_summary");
        } catch (Exception e) {
            log.error("Failed to refresh mv_user_notification_summary", e);
        }
    }

    /**
     * Refresh all materialized views on-demand
     * Can be called via API endpoint or admin console
     */
    public void refreshAllViews() {
        log.info("Starting refresh of all materialized views");
        refreshPatientClinicalSummary();
        refreshBillingSummary();
        refreshNotificationSummary();
        log.info("Completed refresh of all materialized views");
    }
}
```

**Step 3: Enable Scheduling in Application**

Ensure `@EnableScheduling` is present in your main application class:

```java
package com.clinic.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Add this annotation
public class ClinicBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicBackendApplication.class, args);
    }
}
```

**Step 4: Configure Scheduling in application.yml (Optional)**

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 5  # Number of threads for scheduled tasks
      thread-name-prefix: "mv-refresh-"
```

### Advantages:
-  No PostgreSQL extension required
-  Centralized application logging
-  Easy to monitor and debug
-  Works with any PostgreSQL version
-  Can be controlled via application properties
-  Integrated with Spring Boot health checks

### Disadvantages:
- ❌ Application must be running for refreshes
- ❌ No refresh during application downtime

---

## Option 2: Manual Refresh via SQL

Manually refresh views as needed using SQL commands.

### Commands:

```sql
-- Refresh all views
SELECT refresh_patient_clinical_summary();
SELECT refresh_billing_summary();
SELECT refresh_notification_summary();

-- Or using direct REFRESH command
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_patient_clinical_summary;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_billing_summary_by_period;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_user_notification_summary;
```

### Via Docker:

```bash
# Refresh all views
docker exec clinic-postgres psql -U clinic_user -d clinic -c "
SELECT refresh_patient_clinical_summary();
SELECT refresh_billing_summary();
SELECT refresh_notification_summary();"
```

### Advantages:
-  Full control over refresh timing
-  No additional setup required
-  Good for development/testing

### Disadvantages:
- ❌ Requires manual intervention
- ❌ Easy to forget
- ❌ Not suitable for production

---

## Option 3: Operating System Cron Jobs

Use the host system's cron to schedule database refreshes.

### Setup:

Create a shell script: `/home/sanath/refresh_materialized_views.sh`

```bash
#!/bin/bash
# Refresh Clinic Management Materialized Views

LOG_FILE="/home/sanath/logs/mv_refresh.log"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$DATE] Starting materialized view refresh..." >> "$LOG_FILE"

docker exec clinic-postgres psql -U clinic_user -d clinic -c "
SELECT refresh_patient_clinical_summary();
SELECT refresh_billing_summary();
SELECT refresh_notification_summary();
" >> "$LOG_FILE" 2>&1

if [ $? -eq 0 ]; then
    echo "[$DATE] Successfully refreshed all views" >> "$LOG_FILE"
else
    echo "[$DATE] ERROR: Failed to refresh views" >> "$LOG_FILE"
fi
```

Make executable:
```bash
chmod +x /home/sanath/refresh_materialized_views.sh
mkdir -p /home/sanath/logs
```

Add to crontab:
```bash
crontab -e

# Add these lines:
# Refresh patient summary every 15 minutes
*/15 * * * * /home/sanath/refresh_materialized_views.sh patient

# Refresh billing summary every hour
0 * * * * /home/sanath/refresh_materialized_views.sh billing

# Refresh notification summary every 5 minutes
*/5 * * * * /home/sanath/refresh_materialized_views.sh notification
```

### Advantages:
-  Independent of application
-  Runs even when application is down
-  Leverages standard system tools

### Disadvantages:
- ❌ Requires host system access
- ❌ Logs separate from application
- ❌ More complex deployment

---

## Option 4: Install pg_cron in PostgreSQL Container

Install pg_cron extension in the PostgreSQL container for native database-level scheduling.

### Method A: Modify Running Container (Temporary)

```bash
# 1. Install pg_cron in running container
docker exec -it clinic-postgres bash -c "
apt-get update &&
apt-get install -y postgresql-16-cron
"

# 2. Add to postgresql.conf
docker exec clinic-postgres bash -c "
echo \"shared_preload_libraries = 'pg_cron'\" >> /var/lib/postgresql/data/postgresql.conf
echo \"cron.database_name = 'clinic'\" >> /var/lib/postgresql/data/postgresql.conf
"

# 3. Restart PostgreSQL container
docker restart clinic-postgres

# 4. Enable extension
docker exec clinic-postgres psql -U clinic_user -d clinic -c "CREATE EXTENSION pg_cron;"

# 5. Apply schedules
docker exec -i clinic-postgres psql -U clinic_user -d clinic < clinic-migrations/src/main/resources/db/setup_pg_cron_schedules.sql
```

**WARNING:** Changes lost on container rebuild.

### Method B: Create Custom Dockerfile (Permanent)

Create `Dockerfile.postgres`:

```dockerfile
FROM postgres:16

# Install pg_cron extension
RUN apt-get update && \
    apt-get install -y postgresql-16-cron && \
    rm -rf /var/lib/apt/lists/*

# Configure postgresql.conf
RUN echo "shared_preload_libraries = 'pg_cron'" >> /usr/share/postgresql/postgresql.conf.sample && \
    echo "cron.database_name = 'clinic'" >> /usr/share/postgresql/postgresql.conf.sample
```

Rebuild container:
```bash
docker build -f Dockerfile.postgres -t clinic-postgres:pg_cron .
docker stop clinic-postgres
docker rm clinic-postgres
docker run -d --name clinic-postgres -p 5432:5432 -e POSTGRES_USER=clinic_user -e POSTGRES_DB=clinic clinic-postgres:pg_cron
```

Then apply schedules:
```bash
docker exec -i clinic-postgres psql -U clinic_user -d clinic < clinic-migrations/src/main/resources/db/setup_pg_cron_schedules.sql
```

### Advantages:
-  Native database-level scheduling
-  Independent of application
-  Minimal overhead
-  Survives application restarts

### Disadvantages:
- ❌ Requires container modification
- ❌ More complex setup
- ❌ Logs stored in database

---

## Recommendation

**For Production: Use Option 1 (Spring Boot @Scheduled)**

- Easiest to implement
- Best integration with application monitoring
- No infrastructure changes required
- Centralized logging and error handling

**For Development: Use Option 2 (Manual Refresh)**

- Simplest approach
- Full control during testing
- No setup overhead

**For High-Availability: Consider Option 4 (pg_cron)**

- If application may have downtime
- For multi-instance deployments
- When database-level automation is critical

---

## Monitoring Refresh Status

### Check Last Refresh Time:

```sql
SELECT
    matviewname,
    last_refresh
FROM pg_matviews
WHERE matviewname IN (
    'mv_patient_clinical_summary',
    'mv_billing_summary_by_period',
    'mv_user_notification_summary'
);
```

### Check View Sizes:

```sql
SELECT
    schemaname,
    matviewname AS view_name,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||matviewname)) AS size,
    pg_total_relation_size(schemaname||'.'||matviewname) AS size_bytes
FROM pg_matviews
WHERE matviewname IN (
    'mv_patient_clinical_summary',
    'mv_billing_summary_by_period',
    'mv_user_notification_summary'
)
ORDER BY size_bytes DESC;
```

### Application Health Check Endpoint:

```java
@RestController
@RequestMapping("/api/admin/materialized-views")
@RequiredArgsConstructor
public class MaterializedViewAdminController {

    private final MaterializedViewRefreshRepository refreshRepository;

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshAllViews() {
        try {
            refreshRepository.refreshPatientClinicalSummary();
            refreshRepository.refreshBillingSummary();
            refreshRepository.refreshNotificationSummary();
            return ResponseEntity.ok("All materialized views refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to refresh views: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> getViewStatus() {
        // Query pg_matviews for last refresh times
        // Return status information
    }
}
```

---

## Next Steps

1. **Implement Option 1** - Create Spring Boot scheduled tasks (recommended)
2. **Test refresh functions** - Verify views refresh correctly
3. **Monitor performance** - Track refresh duration and view sizes
4. **Set up alerting** - Notify on refresh failures

For questions or issues, refer to the main README at:
`clinic-migrations/src/main/resources/db/MATERIALIZED_VIEWS_README.md`
