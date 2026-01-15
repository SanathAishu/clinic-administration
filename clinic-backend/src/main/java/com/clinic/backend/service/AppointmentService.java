package com.clinic.backend.service;

import com.clinic.common.entity.clinical.Appointment;
import com.clinic.common.entity.core.User;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.common.enums.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

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
        appointment.confirm();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Confirmed appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment startAppointment(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        appointment.start();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Started appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment completeAppointment(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        appointment.complete();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Completed appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment cancelAppointment(UUID id, UUID tenantId, User cancelledBy, String reason) {
        Appointment appointment = getAppointmentById(id, tenantId);
        appointment.cancel(cancelledBy, reason);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Cancelled appointment: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Appointment markNoShow(UUID id, UUID tenantId) {
        Appointment appointment = getAppointmentById(id, tenantId);
        appointment.markNoShow();
        Appointment saved = appointmentRepository.save(appointment);
        log.warn("Marked appointment as no-show: {}", saved.getId());
        return saved;
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
