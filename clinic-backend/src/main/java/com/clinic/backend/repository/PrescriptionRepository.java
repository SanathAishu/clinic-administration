package com.clinic.backend.repository;

import com.clinic.common.entity.clinical.Prescription;
import com.clinic.common.enums.PrescriptionStatus;
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
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    // Tenant-scoped queries
    Page<Prescription> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Prescription> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Patient prescriptions
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.tenantId = :tenantId AND " +
           "p.deletedAt IS NULL ORDER BY p.prescriptionDate DESC")
    Page<Prescription> findPatientPrescriptions(@Param("patientId") UUID patientId,
                                                 @Param("tenantId") UUID tenantId,
                                                 Pageable pageable);

    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.tenantId = :tenantId AND " +
           "p.deletedAt IS NULL ORDER BY p.prescriptionDate DESC")
    Page<Prescription> findByPatientIdAndTenantIdAndDeletedAtIsNull(@Param("patientId") UUID patientId,
                                                                     @Param("tenantId") UUID tenantId,
                                                                     Pageable pageable);

    List<Prescription> findByPatientIdAndTenantIdAndStatusAndDeletedAtIsNull(UUID patientId, UUID tenantId, PrescriptionStatus status);

    // Medical record prescriptions
    List<Prescription> findByMedicalRecordIdAndTenantIdAndDeletedAtIsNull(UUID medicalRecordId, UUID tenantId);

    // Doctor prescriptions
    @Query("SELECT p FROM Prescription p WHERE p.doctor.id = :doctorId AND p.tenantId = :tenantId AND " +
           "p.deletedAt IS NULL ORDER BY p.prescriptionDate DESC")
    Page<Prescription> findByDoctorId(@Param("doctorId") UUID doctorId,
                                       @Param("tenantId") UUID tenantId,
                                       Pageable pageable);

    @Query("SELECT p FROM Prescription p WHERE p.doctor.id = :doctorId AND p.tenantId = :tenantId AND " +
           "p.deletedAt IS NULL ORDER BY p.prescriptionDate DESC")
    List<Prescription> findByDoctorIdAndTenantId(@Param("doctorId") UUID doctorId, @Param("tenantId") UUID tenantId);

    // Status-based queries
    List<Prescription> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, PrescriptionStatus status);

    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.tenantId = :tenantId AND " +
           "p.status = 'PENDING' AND p.deletedAt IS NULL")
    List<Prescription> findActivePrescriptionsForPatient(@Param("patientId") UUID patientId,
                                                          @Param("tenantId") UUID tenantId);

    // Date range queries
    @Query("SELECT p FROM Prescription p WHERE p.tenantId = :tenantId AND " +
           "p.prescriptionDate BETWEEN :startDate AND :endDate AND p.deletedAt IS NULL")
    List<Prescription> findByDateRange(@Param("tenantId") UUID tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // Prescriptions by date range (renamed from expiring - entity doesn't have validUntil)
    @Query("SELECT p FROM Prescription p WHERE p.tenantId = :tenantId AND p.status = 'PENDING' AND " +
           "p.prescriptionDate BETWEEN :today AND :futureDate AND p.deletedAt IS NULL")
    List<Prescription> findExpiringPrescriptions(@Param("tenantId") UUID tenantId,
                                                  @Param("today") LocalDate today,
                                                  @Param("futureDate") LocalDate futureDate);

    // Counting
    long countByPatientIdAndTenantIdAndStatusAndDeletedAtIsNull(UUID patientId, UUID tenantId, PrescriptionStatus status);

    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.patient.id = :patientId AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    long countByPatientIdAndTenantId(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);
}
