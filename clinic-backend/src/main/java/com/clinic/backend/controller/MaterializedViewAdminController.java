package com.clinic.backend.controller;

import com.clinic.backend.service.MaterializedViewRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for materialized view management.
 *
 * Provides endpoints for:
 * - Manual refresh of materialized views
 * - View status monitoring
 * - Performance metrics
 *
 * Access should be restricted to admin users only via security configuration.
 */
@RestController
@RequestMapping("/api/admin/materialized-views")
@RequiredArgsConstructor
@Tag(name = "Materialized Views Admin", description = "Endpoints for managing materialized views")
public class MaterializedViewAdminController {

    private final MaterializedViewRefreshService refreshService;

    /**
     * Manually refresh all materialized views.
     *
     * This endpoint triggers an immediate refresh of all Phase 1 materialized views:
     * - mv_patient_clinical_summary
     * - mv_billing_summary_by_period
     * - mv_user_notification_summary
     *
     * Use cases:
     * - After bulk data import
     * - After data migration
     * - Testing and development
     * - Recovery from refresh failures
     *
     * @return Success message with refresh status
     */
    @PostMapping("/refresh/all")
    @Operation(summary = "Refresh all materialized views", description = "Triggers immediate refresh of all Phase 1 materialized views")
    public ResponseEntity<String> refreshAllViews() {
        try {
            long startTime = System.currentTimeMillis();
            refreshService.refreshAllViews();
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(
                String.format("Successfully refreshed all materialized views in %dms", duration)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to refresh views: " + e.getMessage());
        }
    }

    /**
     * Manually refresh patient clinical summary view.
     *
     * @return Success message with refresh status
     */
    @PostMapping("/refresh/patient-summary")
    @Operation(summary = "Refresh patient clinical summary view")
    public ResponseEntity<String> refreshPatientSummary() {
        try {
            long startTime = System.currentTimeMillis();
            refreshService.refreshPatientClinicalSummary();
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(
                String.format("Successfully refreshed mv_patient_clinical_summary in %dms", duration)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to refresh patient summary: " + e.getMessage());
        }
    }

    /**
     * Manually refresh billing summary view.
     *
     * @return Success message with refresh status
     */
    @PostMapping("/refresh/billing-summary")
    @Operation(summary = "Refresh billing summary view")
    public ResponseEntity<String> refreshBillingSummary() {
        try {
            long startTime = System.currentTimeMillis();
            refreshService.refreshBillingSummary();
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(
                String.format("Successfully refreshed mv_billing_summary_by_period in %dms", duration)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to refresh billing summary: " + e.getMessage());
        }
    }

    /**
     * Manually refresh notification summary view.
     *
     * @return Success message with refresh status
     */
    @PostMapping("/refresh/notification-summary")
    @Operation(summary = "Refresh notification summary view")
    public ResponseEntity<String> refreshNotificationSummary() {
        try {
            long startTime = System.currentTimeMillis();
            refreshService.refreshNotificationSummary();
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(
                String.format("Successfully refreshed mv_user_notification_summary in %dms", duration)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to refresh notification summary: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for materialized views.
     *
     * Can be extended to return:
     * - Last refresh timestamps
     * - View sizes
     * - Row counts
     * - Refresh status
     *
     * @return Health status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check for materialized views")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Materialized view refresh service is running");
    }
}
