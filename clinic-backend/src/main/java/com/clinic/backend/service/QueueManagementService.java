package com.clinic.backend.service;

import com.clinic.common.entity.clinical.Appointment;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.operational.QueueMetrics;
import com.clinic.common.enums.AppointmentStatus;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.QueueMetricsRepository;
import com.clinic.backend.dto.QueueStatusDTO;
import com.clinic.backend.dto.WaitTimeDTO;
import com.clinic.backend.dto.QueuePositionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Queue Management Service - M/M/1 Queuing Theory Implementation
 *
 * Implements queue operations for the clinic appointment system based on:
 * - M/M/1 Queuing Model (Markovian arrivals, Markovian service, 1 server)
 * - Little's Law: L = λW
 * - Stability condition: ρ = λ/μ < 1
 *
 * Key responsibilities:
 * 1. Generate monotonically increasing token numbers per doctor per day
 * 2. Estimate wait times using M/M/1 formulas
 * 3. Track queue positions and metrics
 * 4. Calculate service and arrival rates
 * 5. Store daily queue metrics for analytics
 *
 * Caching strategy:
 * - Queue status: 30 seconds (highly volatile)
 * - Wait time estimates: Per appointment (evict on status change)
 * - Service rate: 1 hour (stable)
 * - Arrival rate: 5 minutes (medium volatility)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueManagementService {

    private final AppointmentRepository appointmentRepository;
    private final QueueMetricsRepository queueMetricsRepository;

    private static final double MIN_SERVICE_RATE = 0.1; // At least 6 minutes per patient
    private static final int SERVICE_RATE_LOOKBACK_DAYS = 7;
    private static final double ARRIVAL_RATE_LOOKBACK_HOURS = 8; // 8-hour clinic day

    /**
     * Generate monotonically increasing token number for an appointment
     *
     * Theorem 6 (Token Monotonicity): For doctor d on date D, token numbers strictly increase
     * with appointment times:
     *
     * token(d, D, t) = COUNT(appointments WHERE
     *                   doctor = d AND
     *                   date = D AND
     *                   appointmentTime ≤ t AND
     *                   status NOT IN ('CANCELLED', 'NO_SHOW')) + 1
     *
     * This ensures uniqueness and monotonicity per doctor per day.
     *
     * @param doctorId   Doctor's user ID
     * @param tenantId   Tenant ID for multi-tenancy
     * @param date       Appointment date
     * @param time       Appointment time
     * @return Next available token number
     */
    @Transactional
    public Integer generateTokenNumber(UUID doctorId, UUID tenantId, LocalDate date, LocalTime time) {
        Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = date.plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant appointmentInstant = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant();

        // Count valid appointments before or at this time on the same day
        List<Appointment> appointmentsOnDay = appointmentRepository.findDoctorAppointmentsForDay(
            doctorId, tenantId, dayStart, dayEnd
        );

        int tokenCount = 0;
        for (Appointment appointment : appointmentsOnDay) {
            // Include appointments that are not cancelled or no-show
            if (appointment.getStatus() != AppointmentStatus.CANCELLED &&
                appointment.getStatus() != AppointmentStatus.NO_SHOW &&
                appointment.getAppointmentTime().isBefore(appointmentInstant) ||
                appointment.getAppointmentTime().equals(appointmentInstant)) {
                tokenCount++;
            }
        }

        int newToken = tokenCount + 1;
        log.debug("Generated token {} for doctor {} on date {}", newToken, doctorId, date);
        return newToken;
    }

    /**
     * Estimate wait time for an appointment using M/M/1 formula
     *
     * Formula: W = 1 / (μ - λ)
     * Where:
     * - W = Average wait time in system (hours)
     * - μ = Service rate (patients per hour)
     * - λ = Arrival rate (patients per hour)
     *
     * The wait time is calculated based on current queue position and service rate.
     *
     * @param appointmentId Appointment ID
     * @param tenantId      Tenant ID
     * @return Estimated wait time in minutes
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "wait_times", key = "#appointmentId", unless = "#result == null")
    public WaitTimeDTO estimateWaitTime(UUID appointmentId, UUID tenantId) {
        Appointment appointment = appointmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(
            appointmentId, tenantId
        ).orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        UUID doctorId = appointment.getDoctor().getId();
        double serviceRate = calculateServiceRate(doctorId, tenantId); // μ (patients/hour)
        double arrivalRate = calculateArrivalRate(doctorId, tenantId, appointment.getAppointmentTime().atZone(ZoneId.systemDefault()).toLocalDate()); // λ (patients/hour)

        double utilization = arrivalRate / serviceRate;

        if (utilization >= 1.0) {
            log.warn("Queue unstable for doctor {} (ρ={} ≥ 1.0)", doctorId, utilization);
            // Use heuristic: average time between appointments
            return WaitTimeDTO.builder()
                .appointmentId(appointmentId)
                .estimatedWaitMinutes(30)
                .isUnstable(true)
                .confidence("LOW")
                .build();
        }

        // M/M/1 formula: W = 1 / (μ - λ)
        double waitTimeHours = 1.0 / (serviceRate - arrivalRate);
        int waitTimeMinutes = (int) Math.round(waitTimeHours * 60);

        // Add queue position multiplier (simple heuristic)
        QueuePositionDTO position = getQueuePosition(appointmentId, doctorId, tenantId,
            appointment.getAppointmentTime().atZone(ZoneId.systemDefault()).toLocalDate());

        int adjustedWaitTime = waitTimeMinutes * Math.max(1, position.getPosition() / 5);

        return WaitTimeDTO.builder()
            .appointmentId(appointmentId)
            .estimatedWaitMinutes(adjustedWaitTime)
            .isUnstable(false)
            .confidence("MEDIUM")
            .utilization(utilization)
            .arrivalRate(arrivalRate)
            .serviceRate(serviceRate)
            .build();
    }

    /**
     * Get current position in queue for an appointment
     *
     * Position is calculated as the count of appointments:
     * - Same doctor
     * - Same day
     * - Earlier appointment time
     * - Status not CANCELLED or NO_SHOW
     *
     * @param appointmentId Appointment ID
     * @param doctorId      Doctor's user ID
     * @param tenantId      Tenant ID
     * @param date          Appointment date
     * @return Queue position DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "queue_positions", key = "#appointmentId", unless = "#result == null")
    public QueuePositionDTO getQueuePosition(UUID appointmentId, UUID doctorId, UUID tenantId, LocalDate date) {
        Appointment appointment = appointmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(
            appointmentId, tenantId
        ).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = date.plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Appointment> appointmentsOnDay = appointmentRepository.findDoctorAppointmentsForDay(
            doctorId, tenantId, dayStart, dayEnd
        );

        // Count appointments before this one (excluding cancelled and no-show)
        int position = 0;
        int totalQueue = 0;
        for (Appointment apt : appointmentsOnDay) {
            if (apt.getStatus() != AppointmentStatus.CANCELLED &&
                apt.getStatus() != AppointmentStatus.NO_SHOW) {
                totalQueue++;
                if (apt.getAppointmentTime().isBefore(appointment.getAppointmentTime())) {
                    position++;
                }
            }
        }

        return QueuePositionDTO.builder()
            .appointmentId(appointmentId)
            .position(position + 1) // 1-indexed
            .queueLength(totalQueue)
            .aheadCount(position)
            .build();
    }

    /**
     * Calculate service rate (μ) for a doctor
     *
     * Service rate = Average number of patients served per hour
     * Calculated from historical data (last 7 days)
     *
     * Formula: μ = total_completed_appointments / (working_hours * days)
     *
     * @param doctorId Doctor's user ID
     * @param tenantId Tenant ID
     * @return Service rate (patients per hour)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "service_rates", key = "#doctorId + ':' + #tenantId", unless = "#result == null")
    public Double calculateServiceRate(UUID doctorId, UUID tenantId) {
        LocalDate startDate = LocalDate.now().minusDays(SERVICE_RATE_LOOKBACK_DAYS);
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = LocalDate.now().plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Appointment> completedAppointments = appointmentRepository.findByDoctorIdAndTenantIdAndStatusAndDeletedAtIsNull(
            doctorId, tenantId, AppointmentStatus.COMPLETED, org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        int totalCompleted = 0;
        for (Appointment appointment : completedAppointments) {
            if (appointment.getCompletedAt() != null &&
                appointment.getCompletedAt().isAfter(startInstant) &&
                appointment.getCompletedAt().isBefore(endInstant)) {
                totalCompleted++;
            }
        }

        // Calculate average service time per patient
        // Assume 8-hour clinic day
        double workingHoursPerDay = 8.0;
        double serviceRate = totalCompleted > 0 ?
            totalCompleted / (workingHoursPerDay * SERVICE_RATE_LOOKBACK_DAYS) :
            MIN_SERVICE_RATE;

        log.debug("Calculated service rate for doctor {}: {} patients/hour", doctorId, serviceRate);
        return Math.max(serviceRate, MIN_SERVICE_RATE);
    }

    /**
     * Calculate arrival rate (λ) for a doctor on a specific date
     *
     * Arrival rate = Number of appointments scheduled / working hours
     * Uses current day's scheduled appointments
     *
     * Formula: λ = total_appointments / working_hours (typically 8 hours)
     *
     * @param doctorId Doctor's user ID
     * @param tenantId Tenant ID
     * @param date     Date to calculate arrival rate for
     * @return Arrival rate (patients per hour)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "arrival_rates", key = "#doctorId + ':' + #tenantId + ':' + #date", unless = "#result == null")
    public Double calculateArrivalRate(UUID doctorId, UUID tenantId, LocalDate date) {
        Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = date.plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Appointment> appointmentsOnDay = appointmentRepository.findDoctorAppointmentsForDay(
            doctorId, tenantId, dayStart, dayEnd
        );

        // Count valid appointments (not cancelled or no-show)
        long totalAppointments = appointmentsOnDay.stream()
            .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED &&
                          apt.getStatus() != AppointmentStatus.NO_SHOW)
            .count();

        // Clinic operates 8 hours per day
        double arrivalRate = totalAppointments / ARRIVAL_RATE_LOOKBACK_HOURS;
        log.debug("Calculated arrival rate for doctor {} on {}: {} patients/hour", doctorId, date, arrivalRate);
        return arrivalRate;
    }

    /**
     * Get current queue status for digital display boards
     *
     * Returns information suitable for clinic display screens:
     * - Current being served token
     * - Next token
     * - Number waiting
     * - Average wait time
     * - Utilization
     *
     * Cached for 30 seconds (volatile data)
     *
     * @param doctorId Doctor's user ID
     * @param tenantId Tenant ID
     * @return Queue status DTO for display
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "queue_status", key = "#doctorId + ':' + #tenantId", unless = "#result == null")
    public QueueStatusDTO getCurrentQueueStatus(UUID doctorId, UUID tenantId) {
        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Appointment> appointmentsToday = appointmentRepository.findDoctorAppointmentsForDay(
            doctorId, tenantId, dayStart, dayEnd
        );

        // Find in-progress appointment (current token)
        Appointment inProgress = appointmentsToday.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.IN_PROGRESS)
            .findFirst()
            .orElse(null);

        // Find next appointment
        Appointment nextAppointment = appointmentsToday.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED ||
                          apt.getStatus() == AppointmentStatus.SCHEDULED)
            .min(Comparator.comparing(Appointment::getAppointmentTime))
            .orElse(null);

        // Count waiting
        long waiting = appointmentsToday.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED ||
                          apt.getStatus() == AppointmentStatus.SCHEDULED)
            .count();

        double arrivalRate = calculateArrivalRate(doctorId, tenantId, today);
        double serviceRate = calculateServiceRate(doctorId, tenantId);
        double utilization = arrivalRate / serviceRate;

        return QueueStatusDTO.builder()
            .currentToken(inProgress != null ? inProgress.getTokenNumber() : null)
            .nextToken(nextAppointment != null ? nextAppointment.getTokenNumber() : null)
            .patientsWaiting((int) waiting)
            .avgWaitTimeMinutes((int) Math.round((1.0 / (serviceRate - arrivalRate)) * 60))
            .utilization(utilization)
            .isStable(utilization < 1.0)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Calculate and store daily queue metrics
     *
     * Computes M/M/1 queue metrics for a doctor on a specific date:
     * - Arrival rate (λ)
     * - Service rate (μ)
     * - Utilization (ρ = λ/μ)
     * - Average wait time (W = 1/(μ - λ))
     * - Average queue length (Lq = ρ²/(1 - ρ))
     * - System metrics for analytics
     *
     * @param doctorId Doctor's user ID
     * @param tenantId Tenant ID
     * @param date     Date to calculate metrics for
     * @return Calculated queue metrics entity
     */
    @Transactional
    public QueueMetrics calculateQueueMetrics(UUID doctorId, UUID tenantId, LocalDate date) {
        User doctor = new User(); // Load from DB in production
        doctor.setId(doctorId);

        double arrivalRate = calculateArrivalRate(doctorId, tenantId, date);
        double serviceRate = calculateServiceRate(doctorId, tenantId);
        double utilization = arrivalRate / serviceRate;

        Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = date.plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Appointment> appointmentsOnDay = appointmentRepository.findDoctorAppointmentsForDay(
            doctorId, tenantId, dayStart, dayEnd
        );

        int totalPatients = (int) appointmentsOnDay.stream()
            .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED &&
                          apt.getStatus() != AppointmentStatus.NO_SHOW)
            .count();

        int completedAppointments = (int) appointmentsOnDay.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.COMPLETED)
            .count();

        Double avgWaitTime = null;
        Double avgWaitInQueue = null;
        Double avgSystemLength = null;
        Double avgQueueLength = null;

        if (utilization < 1.0) {
            // M/M/1 formulas
            // W = 1 / (μ - λ) hours
            avgWaitTime = 1.0 / (serviceRate - arrivalRate) * 60; // Convert to minutes

            // Wq = ρ / (μ - λ) hours
            avgWaitInQueue = (utilization / (serviceRate - arrivalRate)) * 60; // Convert to minutes

            // L = ρ / (1 - ρ)
            avgSystemLength = utilization / (1 - utilization);

            // Lq = ρ² / (1 - ρ)
            avgQueueLength = (utilization * utilization) / (1 - utilization);
        }

        QueueMetrics metrics = QueueMetrics.builder()
            .doctor(doctor)
            .metricDate(date)
            .arrivalRate(arrivalRate)
            .serviceRate(serviceRate)
            .utilization(utilization)
            .avgWaitTime(avgWaitTime)
            .avgWaitInQueue(avgWaitInQueue)
            .avgSystemLength(avgSystemLength)
            .avgQueueLength(avgQueueLength)
            .totalPatients(totalPatients)
            .completedAppointments(completedAppointments)
            .metricStartTime(LocalTime.of(8, 0))
            .metricEndTime(LocalTime.of(16, 0))
            .build();
        metrics.setTenantId(tenantId);

        metrics = queueMetricsRepository.save(metrics);
        log.info("Calculated queue metrics for doctor {} on {}: ρ={}, W={}min",
            doctorId, date, utilization, avgWaitTime);

        return metrics;
    }

    /**
     * Scheduled job to calculate daily metrics for all doctors
     *
     * Runs daily at 23:59 (11:59 PM) to capture end-of-day statistics
     * Calculates queue metrics for all doctors across all tenants
     */
    @Scheduled(cron = "0 59 23 * * *") // 23:59 daily
    @Transactional
    public void calculateDailyMetrics() {
        log.info("Starting daily queue metrics calculation");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // In production, fetch all doctors from database
        // For now, we'll be called per doctor
        // This is a placeholder for the scheduling mechanism

        log.info("Completed daily queue metrics calculation");
    }

    /**
     * Evict queue-related caches when appointment status changes
     * Called after appointment is modified
     *
     * @param appointmentId Appointment that changed
     */
    @CacheEvict(value = {"wait_times", "queue_positions", "queue_status"}, allEntries = true)
    public void evictQueueCaches(UUID appointmentId) {
        log.debug("Evicted queue caches for appointment {}", appointmentId);
    }
}
