package com.clinic.backend.repository;

import com.clinic.common.entity.patient.Diagnosis;
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
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    // Tenant-scoped queries
    Optional<Diagnosis> findByIdAndTenantId(UUID id, UUID tenantId);

    // Patient diagnoses
    @Query("SELECT d FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    Page<Diagnosis> findPatientDiagnoses(@Param("patientId") UUID patientId,
                                          @Param("tenantId") UUID tenantId,
                                          Pageable pageable);

    @Query("SELECT d FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findByPatientIdAndTenantIdOrderByDiagnosedAtDesc(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findByPatientId(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    // Medical record diagnoses
    List<Diagnosis> findByMedicalRecordIdAndTenantId(UUID medicalRecordId, UUID tenantId);

    Page<Diagnosis> findByMedicalRecordIdAndTenantId(UUID medicalRecordId, UUID tenantId, Pageable pageable);

    // ICD-10 code queries
    Optional<Diagnosis> findByIcd10CodeAndPatientIdAndTenantId(String icd10Code, UUID patientId, UUID tenantId);

    @Query("SELECT d FROM Diagnosis d WHERE d.tenantId = :tenantId AND d.icd10Code = :icd10Code " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findByIcd10Code(@Param("tenantId") UUID tenantId, @Param("icd10Code") String icd10Code);

    @Query("SELECT d FROM Diagnosis d WHERE d.icd10Code = :icd10Code AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findByIcd10CodeAndTenantId(@Param("icd10Code") String icd10Code, @Param("tenantId") UUID tenantId);

    // Diagnosis name search
    @Query("SELECT d FROM Diagnosis d WHERE d.tenantId = :tenantId AND " +
           "LOWER(d.diagnosisName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Diagnosis> findByDiagnosisNameContaining(@Param("tenantId") UUID tenantId, @Param("name") String name);

    // Diagnosis type queries
    @Query("SELECT d FROM Diagnosis d WHERE d.patient.id = :patientId AND d.tenantId = :tenantId AND " +
           "d.diagnosisType = :type ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findPatientDiagnosesByType(@Param("patientId") UUID patientId,
                                                @Param("tenantId") UUID tenantId,
                                                @Param("type") String type);

    // Severity queries
    @Query("SELECT d FROM Diagnosis d WHERE d.tenantId = :tenantId AND d.severity = :severity " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findBySeverity(@Param("tenantId") UUID tenantId, @Param("severity") String severity);

    // Doctor diagnoses
    @Query("SELECT d FROM Diagnosis d WHERE d.diagnosedBy.id = :doctorId AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    Page<Diagnosis> findByDoctorId(@Param("doctorId") UUID doctorId,
                                    @Param("tenantId") UUID tenantId,
                                    Pageable pageable);

    // Date range queries
    @Query("SELECT d FROM Diagnosis d WHERE d.tenantId = :tenantId AND " +
           "d.diagnosedAt BETWEEN :startDate AND :endDate ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findByDateRange(@Param("tenantId") UUID tenantId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    // Active diagnoses for patient
    @Query("SELECT d FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findActivePatientDiagnoses(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId " +
           "ORDER BY d.diagnosedAt DESC")
    List<Diagnosis> findActiveDiagnosesForPatient(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    // Counting
    @Query("SELECT COUNT(d) FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId")
    long countByPatientIdAndTenantId(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(d) FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId")
    long countByPatientId(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(d) FROM Diagnosis d JOIN d.medicalRecord m WHERE m.patient.id = :patientId AND d.tenantId = :tenantId")
    long countActivePatientDiagnoses(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);
}
