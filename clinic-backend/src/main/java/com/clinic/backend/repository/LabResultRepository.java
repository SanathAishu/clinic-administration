package com.clinic.backend.repository;

import com.clinic.common.entity.clinical.LabResult;
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
public interface LabResultRepository extends JpaRepository<LabResult, UUID> {

    // Lab test results (One-to-Many relationship)
    List<LabResult> findByLabTestId(UUID labTestId);

    @Query("SELECT lr FROM LabResult lr WHERE lr.labTest.id = :labTestId ORDER BY lr.resultDate DESC, lr.createdAt DESC")
    List<LabResult> findByLabTestIdOrdered(@Param("labTestId") UUID labTestId);

    // Abnormal results
    @Query("SELECT lr FROM LabResult lr WHERE lr.labTest.id = :labTestId AND lr.isAbnormal = true")
    List<LabResult> findAbnormalResultsByLabTest(@Param("labTestId") UUID labTestId);

    // Patient results by parameter
    @Query("SELECT lr FROM LabResult lr WHERE lr.labTest.patient.id = :patientId AND " +
           "lr.labTest.tenantId = :tenantId AND LOWER(lr.parameterName) = LOWER(:parameterName) AND " +
           "lr.labTest.deletedAt IS NULL ORDER BY lr.resultDate DESC")
    List<LabResult> findPatientResultsByParameter(@Param("patientId") UUID patientId,
                                                   @Param("tenantId") UUID tenantId,
                                                   @Param("parameterName") String parameterName);

    // All abnormal results for patient
    @Query("SELECT lr FROM LabResult lr WHERE lr.labTest.patient.id = :patientId AND " +
           "lr.labTest.tenantId = :tenantId AND lr.isAbnormal = true AND " +
           "lr.labTest.deletedAt IS NULL ORDER BY lr.resultDate DESC")
    List<LabResult> findPatientAbnormalResults(@Param("patientId") UUID patientId,
                                                @Param("tenantId") UUID tenantId);

    // Count results
    long countByLabTestId(UUID labTestId);

    @Query("SELECT COUNT(lr) FROM LabResult lr WHERE lr.labTest.id = :labTestId AND lr.isAbnormal = true")
    long countAbnormalResultsByLabTest(@Param("labTestId") UUID labTestId);

    // Additional methods for service
    @Query("SELECT r FROM LabResult r WHERE r.labTest.id = :labTestId AND r.labTest.tenantId = :tenantId")
    Page<LabResult> findByLabTestIdAndTenantId(@Param("labTestId") UUID labTestId,
                                                @Param("tenantId") UUID tenantId,
                                                Pageable pageable);

    @Query("SELECT r FROM LabResult r JOIN r.labTest l WHERE l.patient.id = :patientId AND l.tenantId = :tenantId AND r.isAbnormal = true")
    List<LabResult> findAbnormalResultsForPatient(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM LabResult r JOIN r.labTest l WHERE l.patient.id = :patientId AND l.tenantId = :tenantId AND r.resultDate >= :since ORDER BY r.resultDate DESC")
    List<LabResult> findRecentResultsForPatient(@Param("patientId") UUID patientId,
                                                 @Param("tenantId") UUID tenantId,
                                                 @Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM LabResult r WHERE r.labTest.id = :labTestId AND r.labTest.tenantId = :tenantId")
    long countByLabTestIdAndTenantId(@Param("labTestId") UUID labTestId, @Param("tenantId") UUID tenantId);
}
