package com.clinic.backend.repository;

import com.clinic.common.entity.clinical.LabTest;
import com.clinic.common.enums.LabTestStatus;
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
public interface LabTestRepository extends JpaRepository<LabTest, UUID> {

    // Tenant-scoped queries
    Page<LabTest> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<LabTest> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Patient lab tests
    @Query("SELECT l FROM LabTest l WHERE l.patient.id = :patientId AND l.tenantId = :tenantId AND " +
           "l.deletedAt IS NULL ORDER BY l.orderedAt DESC")
    Page<LabTest> findPatientLabTests(@Param("patientId") UUID patientId,
                                       @Param("tenantId") UUID tenantId,
                                       Pageable pageable);

    List<LabTest> findByPatientIdAndTenantIdAndStatusAndDeletedAtIsNull(UUID patientId, UUID tenantId, LabTestStatus status);

    // Medical record tests
    List<LabTest> findByMedicalRecordIdAndTenantIdAndDeletedAtIsNull(UUID medicalRecordId, UUID tenantId);

    // Ordered by doctor
    @Query("SELECT l FROM LabTest l WHERE l.orderedBy.id = :doctorId AND l.tenantId = :tenantId AND " +
           "l.deletedAt IS NULL ORDER BY l.orderedAt DESC")
    Page<LabTest> findByOrderedByDoctor(@Param("doctorId") UUID doctorId,
                                         @Param("tenantId") UUID tenantId,
                                         Pageable pageable);

    // Status-based queries (State machine)
    List<LabTest> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, LabTestStatus status);

    Page<LabTest> findByTenantIdAndStatusInAndDeletedAtIsNull(UUID tenantId, List<LabTestStatus> statuses, Pageable pageable);

    // Pending tests
    @Query("SELECT l FROM LabTest l WHERE l.tenantId = :tenantId AND l.status IN ('ORDERED', 'SAMPLE_COLLECTED') AND " +
           "l.deletedAt IS NULL ORDER BY l.orderedAt ASC")
    List<LabTest> findPendingTests(@Param("tenantId") UUID tenantId);

    // Test name search
    @Query("SELECT l FROM LabTest l WHERE l.tenantId = :tenantId AND " +
           "LOWER(l.testName) LIKE LOWER(CONCAT('%', :testName, '%')) AND l.deletedAt IS NULL")
    List<LabTest> findByTestNameContaining(@Param("tenantId") UUID tenantId, @Param("testName") String testName);

    // Date range queries
    @Query("SELECT l FROM LabTest l WHERE l.tenantId = :tenantId AND " +
           "l.orderedAt BETWEEN :startDate AND :endDate AND l.deletedAt IS NULL ORDER BY l.orderedAt DESC")
    List<LabTest> findByOrderedDateRange(@Param("tenantId") UUID tenantId,
                                          @Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    // Overdue tests (in progress for too long)
    @Query("SELECT l FROM LabTest l WHERE l.tenantId = :tenantId AND l.status = 'IN_PROGRESS' AND " +
           "l.sampleCollectedAt < :threshold AND l.deletedAt IS NULL")
    List<LabTest> findOverdueTests(@Param("tenantId") UUID tenantId, @Param("threshold") Instant threshold);

    // Counting
    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, LabTestStatus status);

    long countByPatientIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID tenantId);

    // Additional methods for service
    @Query("SELECT l FROM LabTest l WHERE l.patient.id = :patientId AND l.tenantId = :tenantId AND " +
           "l.deletedAt IS NULL ORDER BY l.orderedAt DESC")
    Page<LabTest> findByPatientIdAndTenantIdAndDeletedAtIsNull(@Param("patientId") UUID patientId,
                                                                @Param("tenantId") UUID tenantId,
                                                                Pageable pageable);

    @Query("SELECT l FROM LabTest l WHERE l.orderedBy.id = :doctorId AND l.tenantId = :tenantId AND l.deletedAt IS NULL")
    List<LabTest> findByOrderedByIdAndTenantId(@Param("doctorId") UUID doctorId, @Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM LabTest l WHERE l.tenantId = :tenantId AND l.status IN ('ORDERED', 'SAMPLE_COLLECTED', 'IN_PROGRESS') AND l.deletedAt IS NULL")
    List<LabTest> findPendingLabTests(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(l) FROM LabTest l WHERE l.patient.id = :patientId AND l.tenantId = :tenantId AND l.deletedAt IS NULL")
    long countByPatientIdAndTenantId(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);
}
