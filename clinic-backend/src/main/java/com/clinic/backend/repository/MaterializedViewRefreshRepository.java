package com.clinic.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for refreshing materialized views.
 *
 * This repository provides methods to refresh the Phase 1 materialized views:
 * - mv_patient_clinical_summary
 * - mv_billing_summary_by_period
 * - mv_user_notification_summary
 *
 * These methods are called by the MaterializedViewRefreshService on a scheduled basis.
 */
@Repository
public interface MaterializedViewRefreshRepository extends JpaRepository<Object, Long> {

    /**
     * Refreshes the patient clinical summary materialized view.
     *
     * This view contains aggregated patient data including latest vitals, diagnoses,
     * prescriptions, and lab tests. Should be refreshed every 15 minutes.
     *
     * Uses REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid blocking reads.
     */
    @Modifying
    @Transactional
    @Query(value = "SELECT refresh_patient_clinical_summary()", nativeQuery = true)
    void refreshPatientClinicalSummary();

    /**
     * Refreshes the billing summary materialized view.
     *
     * This view contains financial aggregations by day/week/month/year.
     * Should be refreshed every hour.
     *
     * Uses REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid blocking reads.
     */
    @Modifying
    @Transactional
    @Query(value = "SELECT refresh_billing_summary()", nativeQuery = true)
    void refreshBillingSummary();

    /**
     * Refreshes the notification summary materialized view.
     *
     * This view contains notification counts and status breakdown per user.
     * Should be refreshed every 5 minutes.
     *
     * Uses REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid blocking reads.
     */
    @Modifying
    @Transactional
    @Query(value = "SELECT refresh_notification_summary()", nativeQuery = true)
    void refreshNotificationSummary();
}
