package com.clinic.backend.repository;

import com.clinic.common.entity.patient.Vital;
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
public interface VitalRepository extends JpaRepository<Vital, UUID> {

    // Tenant-scoped queries
    Optional<Vital> findByIdAndTenantId(UUID id, UUID tenantId);

    // Patient vitals (Temporal sequence)
    @Query("SELECT v FROM Vital v WHERE v.patient.id = :patientId AND v.tenantId = :tenantId " +
           "ORDER BY v.recordedAt DESC")
    Page<Vital> findPatientVitals(@Param("patientId") UUID patientId,
                                   @Param("tenantId") UUID tenantId,
                                   Pageable pageable);

    @Query("SELECT v FROM Vital v WHERE v.patient.id = :patientId AND v.tenantId = :tenantId " +
           "ORDER BY v.recordedAt DESC")
    Page<Vital> findByPatientIdAndTenantId(@Param("patientId") UUID patientId,
                                            @Param("tenantId") UUID tenantId,
                                            Pageable pageable);

    @Query("SELECT v FROM Vital v WHERE v.patient.id = :patientId AND v.tenantId = :tenantId " +
           "ORDER BY v.recordedAt DESC")
    List<Vital> findPatientVitalsHistory(@Param("patientId") UUID patientId,
                                          @Param("tenantId") UUID tenantId);

    // Latest vital for patient
    @Query("SELECT v FROM Vital v WHERE v.patient.id = :patientId AND v.tenantId = :tenantId " +
           "ORDER BY v.recordedAt DESC LIMIT 1")
    Optional<Vital> findLatestVitalForPatient(@Param("patientId") UUID patientId,
                                               @Param("tenantId") UUID tenantId);

    // Appointment vitals
    List<Vital> findByAppointmentIdAndTenantId(UUID appointmentId, UUID tenantId);

    Optional<Vital> findByAppointmentIdAndPatientIdAndTenantId(UUID appointmentId, UUID patientId, UUID tenantId);

    // Recorded by
    @Query("SELECT v FROM Vital v WHERE v.recordedBy.id = :userId AND v.tenantId = :tenantId " +
           "ORDER BY v.recordedAt DESC")
    Page<Vital> findByRecordedBy(@Param("userId") UUID userId,
                                  @Param("tenantId") UUID tenantId,
                                  Pageable pageable);

    // Date range queries
    @Query("SELECT v FROM Vital v WHERE v.patient.id = :patientId AND v.tenantId = :tenantId AND " +
           "v.recordedAt BETWEEN :startDate AND :endDate ORDER BY v.recordedAt DESC")
    List<Vital> findPatientVitalsInDateRange(@Param("patientId") UUID patientId,
                                              @Param("tenantId") UUID tenantId,
                                              @Param("startDate") Instant startDate,
                                              @Param("endDate") Instant endDate);

    @Query("SELECT v FROM Vital v WHERE v.patient.id = :patientId AND v.tenantId = :tenantId AND " +
           "v.recordedAt >= :since ORDER BY v.recordedAt DESC")
    List<Vital> findRecentVitalsForPatient(@Param("patientId") UUID patientId,
                                            @Param("tenantId") UUID tenantId,
                                            @Param("since") Instant since);

    // Abnormal vitals (for alerts)
    @Query("SELECT v FROM Vital v WHERE v.tenantId = :tenantId AND v.recordedAt >= :since AND " +
           "(v.temperatureCelsius < 35.0 OR v.temperatureCelsius > 39.0 OR " +
           "v.pulseBpm < 50 OR v.pulseBpm > 120 OR " +
           "v.systolicBp < 90 OR v.systolicBp > 160 OR " +
           "v.diastolicBp < 60 OR v.diastolicBp > 100 OR " +
           "v.oxygenSaturation < 92.0)")
    List<Vital> findAbnormalVitalsSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    // Count vitals
    long countByPatientIdAndTenantId(UUID patientId, UUID tenantId);
}
