package com.clinic.backend.repository;

import com.clinic.backend.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    // Tenant-scoped queries
    Page<MedicalRecord> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<MedicalRecord> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Patient medical history (Temporal sequence)
    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = :patientId AND m.tenantId = :tenantId AND " +
           "m.deletedAt IS NULL ORDER BY m.recordDate DESC, m.createdAt DESC")
    Page<MedicalRecord> findPatientMedicalHistory(@Param("patientId") UUID patientId,
                                                   @Param("tenantId") UUID tenantId,
                                                   Pageable pageable);

    // Appointment-linked records
    Optional<MedicalRecord> findByAppointmentIdAndTenantIdAndDeletedAtIsNull(UUID appointmentId, UUID tenantId);

    List<MedicalRecord> findByPatientIdAndAppointmentIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID appointmentId, UUID tenantId);

    // Doctor records
    @Query("SELECT m FROM MedicalRecord m WHERE m.doctor.id = :doctorId AND m.tenantId = :tenantId AND " +
           "m.deletedAt IS NULL ORDER BY m.recordDate DESC")
    Page<MedicalRecord> findByDoctorId(@Param("doctorId") UUID doctorId,
                                        @Param("tenantId") UUID tenantId,
                                        Pageable pageable);

    // Date range queries
    @Query("SELECT m FROM MedicalRecord m WHERE m.tenantId = :tenantId AND m.recordDate BETWEEN :startDate AND :endDate AND " +
           "m.deletedAt IS NULL ORDER BY m.recordDate DESC")
    List<MedicalRecord> findByDateRange(@Param("tenantId") UUID tenantId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    // Search in clinical notes
    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = :patientId AND m.tenantId = :tenantId AND " +
           "m.deletedAt IS NULL AND (LOWER(m.chiefComplaint) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.clinicalNotes) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.treatmentPlan) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<MedicalRecord> searchPatientRecords(@Param("patientId") UUID patientId,
                                              @Param("tenantId") UUID tenantId,
                                              @Param("search") String search);

    // Count records
    long countByPatientIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID tenantId);

    long countByDoctorIdAndTenantIdAndDeletedAtIsNull(UUID doctorId, UUID tenantId);
}
