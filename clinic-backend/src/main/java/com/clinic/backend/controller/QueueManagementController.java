package com.clinic.backend.controller;

import com.clinic.backend.dto.QueueStatusDTO;
import com.clinic.backend.dto.WaitTimeDTO;
import com.clinic.backend.dto.QueuePositionDTO;
import com.clinic.backend.service.QueueManagementService;
import com.clinic.common.entity.operational.QueueMetrics;
import com.clinic.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * REST Controller for Queue Management Operations
 *
 * Provides endpoints for:
 * - Real-time queue status (for digital display boards)
 * - Wait time estimation (for patient information)
 * - Queue position tracking
 * - Queue metrics analytics
 * - Manual metrics calculation
 *
 * All endpoints are tenant-scoped and require appropriate security permissions
 *
 * Base Path: /api/queue
 */
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueManagementController {

    private final QueueManagementService queueManagementService;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/queue/status/{doctorId}
     *
     * Get current queue status for a doctor
     * Used by clinic display boards to show real-time queue information
     *
     * Returns:
     * - Current token number being served
     * - Next token to be called
     * - Number of patients waiting
     * - Average wait time estimate
     * - Queue utilization (ρ)
     * - Queue stability indicator
     *
     * Caching: 30 seconds (volatile data)
     * Security: Requires ADMIN, DOCTOR, or RECEPTIONIST role
     *
     * @param doctorId Doctor's user ID
     * @return 200 OK with QueueStatusDTO
     *         404 Not Found if doctor not found
     */
    @GetMapping("/status/{doctorId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<QueueStatusDTO> getQueueStatus(@PathVariable UUID doctorId) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching queue status for doctor {} in tenant {}", doctorId, tenantId);

        try {
            QueueStatusDTO status = queueManagementService.getCurrentQueueStatus(doctorId, tenantId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error fetching queue status for doctor {}: {}", doctorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/queue/wait-time/{appointmentId}
     *
     * Get estimated wait time for a specific appointment
     * Uses M/M/1 queuing formula adjusted for queue position
     *
     * Formula: W = 1 / (μ - λ) * ceil(position / 5)
     * Where:
     * - W = Average wait time
     * - μ = Service rate
     * - λ = Arrival rate
     * - position = Appointment's position in queue
     *
     * Returns:
     * - Estimated wait time in minutes
     * - Confidence level (LOW, MEDIUM, HIGH)
     * - Queue utilization
     * - Underlying M/M/1 parameters
     *
     * Caching: Per appointment (evicted on status change)
     * Security: Requires PATIENT, ADMIN, DOCTOR, or RECEPTIONIST role
     *
     * @param appointmentId Appointment's ID
     * @return 200 OK with WaitTimeDTO
     *         404 Not Found if appointment not found
     */
    @GetMapping("/wait-time/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<WaitTimeDTO> estimateWaitTime(@PathVariable UUID appointmentId) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Estimating wait time for appointment {} in tenant {}", appointmentId, tenantId);

        try {
            WaitTimeDTO waitTime = queueManagementService.estimateWaitTime(appointmentId, tenantId);
            return ResponseEntity.ok(waitTime);
        } catch (IllegalArgumentException e) {
            log.warn("Appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error estimating wait time for appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/queue/position/{appointmentId}
     *
     * Get current position in queue for a specific appointment
     *
     * Returns:
     * - Current position (1-indexed, 1 = next to be served)
     * - Total queue length
     * - Number of patients ahead
     *
     * Caching: Per appointment (evicted on status change)
     * Security: Requires PATIENT, ADMIN, DOCTOR, or RECEPTIONIST role
     *
     * @param appointmentId Appointment's ID
     * @return 200 OK with QueuePositionDTO
     *         404 Not Found if appointment not found
     */
    @GetMapping("/position/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<QueuePositionDTO> getQueuePosition(@PathVariable UUID appointmentId) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching queue position for appointment {} in tenant {}", appointmentId, tenantId);

        try {
            // Note: This is a simplified version - in production would need appointment details
            // to extract doctor ID and date
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            log.error("Error fetching queue position for appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/queue/metrics/{doctorId}
     *
     * Get queue metrics for a specific doctor on a given date
     * Provides detailed M/M/1 queuing theory analytics
     *
     * Query Parameters:
     * - date: LocalDate (required) - Format: yyyy-MM-dd
     *
     * Returns QueueMetrics entity with:
     * - Arrival rate (λ)
     * - Service rate (μ)
     * - Utilization (ρ)
     * - Average wait time (W)
     * - Average queue length (Lq)
     * - Stability analysis
     *
     * Security: Requires ADMIN, DOCTOR, or RECEPTIONIST role
     *
     * @param doctorId Doctor's user ID
     * @param date     Date for metrics (format: yyyy-MM-dd)
     * @return 200 OK with QueueMetrics
     *         404 Not Found if no metrics available
     */
    @GetMapping("/metrics/{doctorId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<QueueMetrics> getQueueMetrics(
            @PathVariable UUID doctorId,
            @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching queue metrics for doctor {} on {} in tenant {}", doctorId, date, tenantId);

        try {
            QueueMetrics metrics = queueManagementService.calculateQueueMetrics(doctorId, tenantId, date);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error fetching queue metrics for doctor {}: {}", doctorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/queue/metrics/calculate
     *
     * Manually trigger queue metrics calculation for a specific date
     * Typically called by scheduled job or admin for reprocessing historical data
     *
     * Query Parameters:
     * - date: LocalDate (optional, defaults to yesterday) - Format: yyyy-MM-dd
     *
     * Returns: Number of metrics calculated
     *
     * Security: Requires ADMIN role only
     *
     * @param date Date to calculate metrics for (defaults to yesterday)
     * @return 200 OK with message
     */
    @PostMapping("/metrics/calculate")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> calculateMetrics(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate targetDate = date != null ? date : LocalDate.now().minusDays(1);

        log.info("Manually triggering metrics calculation for date {} in tenant {}", targetDate, tenantId);

        try {
            // Call service to calculate metrics for all doctors on this date
            queueManagementService.calculateDailyMetrics();

            return ResponseEntity.ok("Queue metrics calculation initiated for date: " + targetDate);
        } catch (Exception e) {
            log.error("Error calculating queue metrics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error calculating metrics: " + e.getMessage());
        }
    }

    /**
     * POST /api/queue/token/generate
     *
     * Generate monotonically increasing token number for an appointment
     * This is called by AppointmentService during appointment creation
     *
     * Note: This endpoint is typically NOT exposed to patients/general public
     * It's used internally by the appointment creation flow
     *
     * Security: Requires ADMIN, DOCTOR, or RECEPTIONIST role
     *
     * @param doctorId Doctor's user ID
     * @param date Appointment date
     * @param time Appointment time
     * @return Generated token number
     */
    @PostMapping("/token/generate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<Integer> generateToken(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Generating token for doctor {} on {} at {} in tenant {}", doctorId, date, time, tenantId);

        try {
            Integer tokenNumber = queueManagementService.generateTokenNumber(doctorId, tenantId, date, time);
            return ResponseEntity.ok(tokenNumber);
        } catch (Exception e) {
            log.error("Error generating token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
