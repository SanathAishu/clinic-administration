package com.clinic.backend.repository;

import com.clinic.common.entity.clinical.Appointment;
import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.ConsultationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // Tenant-scoped queries
    Page<Appointment> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Appointment> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Patient appointments
    Page<Appointment> findByPatientIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID tenantId, Pageable pageable);

    List<Appointment> findByPatientIdAndTenantIdAndStatusInAndDeletedAtIsNull(UUID patientId, UUID tenantId, List<AppointmentStatus> statuses);

    // Doctor schedule queries (Combinatorics - validate capacity)
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.tenantId = :tenantId AND " +
           "a.appointmentTime BETWEEN :startTime AND :endTime AND a.status NOT IN :excludedStatuses AND a.deletedAt IS NULL")
    List<Appointment> findDoctorAppointmentsInTimeRange(@Param("doctorId") UUID doctorId,
                                                         @Param("tenantId") UUID tenantId,
                                                         @Param("startTime") Instant startTime,
                                                         @Param("endTime") Instant endTime,
                                                         @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    // Temporal overlap check (enforced at application level due to DB constraint removal)
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.tenantId = :tenantId AND " +
           "a.appointmentTime < :endTime AND " +
           "FUNCTION('TIMESTAMPADD', MINUTE, a.durationMinutes, a.appointmentTime) > :startTime AND " +
           "a.status NOT IN ('CANCELLED', 'NO_SHOW') AND a.deletedAt IS NULL AND a.id != :excludeId")
    long countOverlappingAppointments(@Param("doctorId") UUID doctorId,
                                       @Param("tenantId") UUID tenantId,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime,
                                       @Param("excludeId") UUID excludeId);

    // Status-based queries (State machine)
    List<Appointment> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, AppointmentStatus status);

    Page<Appointment> findByDoctorIdAndTenantIdAndStatusAndDeletedAtIsNull(UUID doctorId, UUID tenantId, AppointmentStatus status, Pageable pageable);

    // Upcoming appointments
    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND a.appointmentTime >= :now AND " +
           "a.status IN :statuses AND a.deletedAt IS NULL ORDER BY a.appointmentTime ASC")
    List<Appointment> findUpcomingAppointments(@Param("tenantId") UUID tenantId,
                                                @Param("now") Instant now,
                                                @Param("statuses") List<AppointmentStatus> statuses);

    // Today's appointments for doctor
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.tenantId = :tenantId AND " +
           "a.appointmentTime BETWEEN :dayStart AND :dayEnd AND a.deletedAt IS NULL ORDER BY a.appointmentTime ASC")
    List<Appointment> findDoctorAppointmentsForDay(@Param("doctorId") UUID doctorId,
                                                    @Param("tenantId") UUID tenantId,
                                                    @Param("dayStart") Instant dayStart,
                                                    @Param("dayEnd") Instant dayEnd);

    // Consultation type queries
    List<Appointment> findByTenantIdAndConsultationTypeAndDeletedAtIsNull(UUID tenantId, ConsultationType type);

    // Pending confirmations
    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND a.status = 'SCHEDULED' AND " +
           "a.confirmedAt IS NULL AND a.appointmentTime > :now AND a.deletedAt IS NULL")
    List<Appointment> findUnconfirmedAppointments(@Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // Counting
    long countByDoctorIdAndTenantIdAndAppointmentTimeBetweenAndStatusNotInAndDeletedAtIsNull(
        UUID doctorId, UUID tenantId, Instant start, Instant end, List<AppointmentStatus> excludedStatuses);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, AppointmentStatus status);
}
