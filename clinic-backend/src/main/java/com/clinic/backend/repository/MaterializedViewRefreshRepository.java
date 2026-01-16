package com.clinic.backend.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Repository for refreshing materialized views.
 *
 * This repository provides methods to refresh the Phase 1 materialized views:
 * - mv_patient_clinical_summary
 * - mv_billing_summary_by_period
 * - mv_user_notification_summary
 *
 * These methods are called by the MaterializedViewRefreshService on a scheduled basis.
 *
 * Note: This repository uses EntityManager for native query execution instead of
 * extending JpaRepository, since it only executes stored procedures and native queries
 * without managing any entities.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MaterializedViewRefreshRepository {

    private final EntityManager entityManager;

    /**
     * Refreshes the patient clinical summary materialized view.
     *
     * This view contains aggregated patient data including latest vitals, diagnoses,
     * prescriptions, and lab tests. Should be refreshed every 15 minutes.
     *
     * Uses REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid blocking reads.
     */
    @Transactional
    public void refreshPatientClinicalSummary() {
        Query query = entityManager.createNativeQuery("SELECT refresh_patient_clinical_summary()");
        query.executeUpdate();
    }

    /**
     * Refreshes the billing summary materialized view.
     *
     * This view contains financial aggregations by day/week/month/year.
     * Should be refreshed every hour.
     *
     * Uses REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid blocking reads.
     */
    @Transactional
    public void refreshBillingSummary() {
        Query query = entityManager.createNativeQuery("SELECT refresh_billing_summary()");
        query.executeUpdate();
    }

    /**
     * Refreshes the notification summary materialized view.
     *
     * This view contains notification counts and status breakdown per user.
     * Should be refreshed every 5 minutes.
     *
     * Uses REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid blocking reads.
     */
    @Transactional
    public void refreshNotificationSummary() {
        Query query = entityManager.createNativeQuery("SELECT refresh_notification_summary()");
        query.executeUpdate();
    }
}
