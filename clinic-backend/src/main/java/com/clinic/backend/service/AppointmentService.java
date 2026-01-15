package com.clinic.backend.service;

import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.AppointmentViewRepository;
import com.clinic.common.dto.view.AppointmentDetailViewDTO;
import com.clinic.common.dto.view.AppointmentListViewDTO;
import com.clinic.common.dto.view.AvailableSlotDTO;
import com.clinic.common.dto.view.TodayAppointmentViewDTO;
import com.clinic.common.entity.clinical.Appointment;
import com.clinic.common.entity.core.User;
import com.clinic.common.enums.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for appointment management implementing CQRS pattern.
 *
 * CQRS Architecture:
 * - READ operations: Use database views via AppointmentViewRepository
 * - WRITE operations: Use JPA entities via AppointmentRepository
 *
 * State Machine (Directed Acyclic Graph - no backwards transitions):
 * SCHEDULED -> CONFIRMED -> IN_PROGRESS -> COMPLETED
 * SCHEDULED -> CANCELLED
 * CONFIRMED -> CANCELLED
 * CONFIRMED -> NO_SHOW
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentViewRepository appointmentViewRepository;

    // ================================
    // Valid Status Transitions (DAG)
    // ================================
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> VALID_TRANSITIONS = Map.of(
            AppointmentStatus.SCHEDULED, Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
            AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.IN_PROGRESS, Set.of(AppointmentStatus.COMPLETED),
            AppointmentStatus.COMPLETED, Set.of(),
            AppointmentStatus.CANCELLED, Set.of(),
            AppointmentStatus.NO_SHOW, Set.of()
    );

    // Default appointment slot duration in minutes
    private static final int DEFAULT_SLOT_DURATION = 30;

    // Working hours (configurable in future)
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    // ================================
    // READ Operations (View-based CQRS)
    // ================================

    /**
     * Get paginated list of appointments using v_appointment_list view.
     */
    public Page<AppointmentListViewDTO> getAppointmentList(UUID tenantId, Pageable pageable) {
        log.debug("Fetching appointment list for tenant: {}", tenantId);
        return appointmentViewRepository.findAllAppointments(tenantId, pageable);
    }

    /**
     * Get appointment list filtered by status.
     */
    public Page<AppointmentListViewDTO> getAppointmentListByStatus(UUID tenantId, AppointmentStatus status, Pageable pageable) {
        log.debug("Fetching appointments by status {} for tenant: {}", status, tenantId);
        return appointmentViewRepository.findByStatus(tenantId, status, pageable);
    }

    /**
     * Get detailed appointment information using v_appointment_detail view.
     */
    public AppointmentDetailViewDTO getAppointmentDetail(UUID tenantId, UUID appointmentId) {
        log.debug("Fetching appointment detail: {} for tenant: {}", appointmentId, tenantId);
        return appointmentViewRepository.findDetailById(tenantId, appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }

    /**
     * Get today's appointments using v_today_appointments view.
     */
    public List<TodayAppointmentViewDTO> getTodayAppointments(UUID tenantId) {
        log.debug("Fetching today's appointments for tenant: {}", tenantId);
        return appointmentViewRepository.findTodayAppointments(tenantId);
    }

    /**
     * Get today's appointments for a specific doctor.
     */
    public List<TodayAppointmentViewDTO> getTodayAppointmentsByDoctor(UUID tenantId, UUID doctorId) {
        log.debug("Fetching today's appointments for doctor: {} in tenant: {}", doctorId, tenantId);
        return appointmentViewRepository.findTodayAppointmentsByDoctor(tenantId, doctorId);
    }

    /**
     * Get appointments for a specific doctor (paginated).
     */
    public Page<AppointmentListViewDTO> getDoctorAppointmentList(UUID tenantId, UUID doctorId, Pageable pageable) {
        log.debug("Fetching appointments for doctor: {} in tenant: {}", doctorId, tenantId);
        return appointmentViewRepository.findByDoctorId(tenantId, doctorId, pageable);
    }

    /**
     * Get appointments for a specific patient (paginated).
     */
    public Page<AppointmentListViewDTO> getPatientAppointmentList(UUID tenantId, UUID patientId, Pageable pageable) {
        log.debug("Fetching appointments for patient: {} in tenant: {}", patientId, tenantId);
        return appointmentViewRepository.findByPatientId(tenantId, patientId, pageable);
    }

    /**
     * Get appointments within a date range.
     */
    public Page<AppointmentListViewDTO> getAppointmentsByDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.debug("Fetching appointments from {} to {} for tenant: {}", startDate, endDate, tenantId);
        return appointmentViewRepository.findByDateRange(tenantId, startDate, endDate, pageable);
    }

    /**
     * Calculate available time slots for a doctor on a specific date.
     * Uses Combinatorics - finding gaps in time intervals.
     */
    public List<AvailableSlotDTO> getAvailableSlots(UUID tenantId, UUID doctorId, LocalDate date, int slotDurationMinutes) {
        log.debug("Calculating available slots for doctor: {} on date: {} in tenant: {}", doctorId, date, tenantId);

        if (slotDurationMinutes <= 0) {
            slotDurationMinutes = DEFAULT_SLOT_DURATION;
        }

        // Get start and end of day in UTC
        ZoneId zoneId = ZoneId.systemDefault();
        Instant dayStart = date.atTime(WORK_START).atZone(zoneId).toInstant();
        Instant dayEnd = date.atTime(WORK_END).atZone(zoneId).toInstant();

        // Fetch existing appointments for the doctor on this day
        List<Appointment> existingAppointments = appointmentRepository.findDoctorAppointmentsForDay(
                doctorId, tenantId, dayStart, dayEnd);

        // Build list of occupied time intervals
        List<long[]> occupiedIntervals = existingAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .map(a -> new long[]{
                        a.getAppointmentTime().toEpochMilli(),
                        a.getAppointmentTime().plus(a.getDurationMinutes(), ChronoUnit.MINUTES).toEpochMilli()
                })
                .sorted(Comparator.comparingLong(a -> a[0]))
                .toList();

        // Generate available slots (gaps between occupied intervals)
        List<AvailableSlotDTO> availableSlots = new ArrayList<>();
        long currentTime = dayStart.toEpochMilli();
        long endOfDay = dayEnd.toEpochMilli();
        long slotDurationMs = slotDurationMinutes * 60 * 1000L;

        for (long[] interval : occupiedIntervals) {
            // Add slots in the gap before this appointment
            while (currentTime + slotDurationMs <= interval[0]) {
                Instant slotStart = Instant.ofEpochMilli(currentTime);
                Instant slotEnd = slotStart.plus(slotDurationMinutes, ChronoUnit.MINUTES);

                availableSlots.add(AvailableSlotDTO.builder()
                        .date(date)
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .startTimeFormatted(formatTime(slotStart, zoneId))
                        .endTimeFormatted(formatTime(slotEnd, zoneId))
                        .durationMinutes(slotDurationMinutes)
                        .isAvailable(true)
                        .build());

                currentTime += slotDurationMs;
            }
            // Move current time past this occupied interval
            currentTime = Math.max(currentTime, interval[1]);
        }

        // Add remaining slots after last appointment
        while (currentTime + slotDurationMs <= endOfDay) {
            Instant slotStart = Instant.ofEpochMilli(currentTime);
            Instant slotEnd = slotStart.plus(slotDurationMinutes, ChronoUnit.MINUTES);

            availableSlots.add(AvailableSlotDTO.builder()
                    .date(date)
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .startTimeFormatted(formatTime(slotStart, zoneId))
                    .endTimeFormatted(formatTime(slotEnd, zoneId))
                    .durationMinutes(slotDurationMinutes)
                    .isAvailable(true)
                    .build());

            currentTime += slotDurationMs;
        }

        // Filter out past slots if date is today
        if (date.equals(LocalDate.now())) {
            Instant now = Instant.now();
            availableSlots = availableSlots.stream()
                    .filter(slot -> slot.getStartTime().isAfter(now))
                    .toList();
        }

        return availableSlots;
    }

    private String formatTime(Instant instant, ZoneId zoneId) {
        return instant.atZone(zoneId).toLocalTime().toString().substring(0, 5);
    }

    @Transactional
    public Appointment createAppointment(Appointment appointment, UUID tenantId) {
        log.debug("Creating appointment for patient: {} with doctor: {}",
                appointment.getPatient().getId(), appointment.getDoctor().getId());

        // Validate temporal overlap (Discrete Math: interval overlap detection)
        Instant endTime = appointment.getAppointmentTime()
                .plus(appointment.getDurationMinutes(), ChronoUnit.MINUTES);

        long overlapping = appointmentRepository.countOverlappingAppointments(
                appointment.getDoctor().getId(),
                tenantId,
                appointment.getAppointmentTime(),
                endTime,
                UUID.randomUUID() // New appointment, exclude nothing
        );

        if (overlapping > 0) {
            throw new IllegalStateException("Doctor schedule conflict: overlapping appointment exists");
        }

        appointment.setTenantId(tenantId);

        if (appointment.getStatus() == null) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }

        if (appointment.getDurationMinutes() == null) {
            appointment.setDurationMinutes(30);
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Created appointment: {}", saved.getId());
        return saved;
    }

    public Appointment getAppointmentById(UUID id, UUID tenantId) {
        return appointmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    }

    public Page<Appointment> getAppointmentsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return appointmentRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<Appointment> getDoctorAppointmentsForDay(UUID doctorId, UUID tenantId, Instant dayStart, Instant dayEnd) {
        return appointmentRepository.findDoctorAppointmentsForDay(doctorId, tenantId, dayStart, dayEnd);
    }

    public List<Appointment> getUpcomingAppointments(UUID tenantId) {
        List<AppointmentStatus> activeStatuses = Arrays.asList(
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.CONFIRMED
        );
        return appointmentRepository.findUpcomingAppointments(tenantId, Instant.now(), activeStatuses);
    }

    public List<Appointment> getUnconfirmedAppointments(UUID tenantId) {
        return appointmentRepository.findUnconfirmedAppointments(tenantId, Instant.now());
    }

    @Transactional
    public Appointment confirmAppointment(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        validateStatusTransition(appointment.getStatus(), AppointmentStatus.CONFIRMED);
        appointment.confirm();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Confirmed appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment startAppointment(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        validateStatusTransition(appointment.getStatus(), AppointmentStatus.IN_PROGRESS);
        appointment.start();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Started appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment completeAppointment(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        validateStatusTransition(appointment.getStatus(), AppointmentStatus.COMPLETED);
        appointment.complete();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Completed appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment cancelAppointment(UUID id, UUID tenantId, User cancelledBy, String reason) {
        Appointment appointment = getAppointmentById(id, tenantId);
        validateStatusTransition(appointment.getStatus(), AppointmentStatus.CANCELLED);
        appointment.cancel(cancelledBy, reason);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Cancelled appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment markNoShow(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        validateStatusTransition(appointment.getStatus(), AppointmentStatus.NO_SHOW);
        appointment.markNoShow();
        Appointment saved = appointmentRepository.save(appointment);
        log.warn("Marked appointment as no-show: {}", saved.getId());
        return saved;
    }

    /**
     * Validates that a status transition is allowed (DAG enforcement).
     * Prevents backwards transitions and invalid state changes.
     *
     * @throws IllegalStateException if transition is not allowed
     */
    private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus targetStatus) {
        Set<AppointmentStatus> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(targetStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s -> %s is not allowed", currentStatus, targetStatus)
            );
        }
    }

    @Transactional
    public Appointment updateAppointment(UUID id, UUID tenantId, Appointment updates) {
        Appointment appointment = getAppointmentById(id, tenantId);

        // Only allow updates if not completed/cancelled
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update completed or cancelled appointment");
        }

        if (updates.getAppointmentTime() != null) {
            Instant newEndTime = updates.getAppointmentTime()
                    .plus(updates.getDurationMinutes() != null ? updates.getDurationMinutes() : appointment.getDurationMinutes(),
                            ChronoUnit.MINUTES);

            long overlapping = appointmentRepository.countOverlappingAppointments(
                    appointment.getDoctor().getId(),
                    tenantId,
                    updates.getAppointmentTime(),
                    newEndTime,
                    appointment.getId()
            );

            if (overlapping > 0) {
                throw new IllegalStateException("Schedule conflict with updated time");
            }

            appointment.setAppointmentTime(updates.getAppointmentTime());
        }

        if (updates.getDurationMinutes() != null) {
            appointment.setDurationMinutes(updates.getDurationMinutes());
        }

        if (updates.getConsultationType() != null) {
            appointment.setConsultationType(updates.getConsultationType());
        }

        if (updates.getReason() != null) {
            appointment.setReason(updates.getReason());
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void softDeleteAppointment(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        appointment.softDelete();
        appointmentRepository.save(appointment);
        log.info("Soft deleted appointment: {}", id);
    }

    public long countAppointmentsByStatus(UUID tenantId, AppointmentStatus status) {
        return appointmentRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }
}
