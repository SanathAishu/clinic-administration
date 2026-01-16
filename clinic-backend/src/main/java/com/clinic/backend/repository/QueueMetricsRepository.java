package com.clinic.backend.repository;

import com.clinic.common.entity.operational.QueueMetrics;
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

/**
 * Repository for QueueMetrics entity
 * Handles persistence of M/M/1 queue statistics for analytics and monitoring
 */
@Repository
public interface QueueMetricsRepository extends JpaRepository<QueueMetrics, UUID> {

    // Tenant-scoped queries
    Page<QueueMetrics> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<QueueMetrics> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Doctor-specific metrics
    @Query("SELECT qm FROM QueueMetrics qm WHERE qm.doctor.id = :doctorId AND qm.tenantId = :tenantId AND " +
           "qm.deletedAt IS NULL ORDER BY qm.metricDate DESC")
    Page<QueueMetrics> findByDoctorAndTenant(@Param("doctorId") UUID doctorId,
                                            @Param("tenantId") UUID tenantId,
                                            Pageable pageable);

    // Metrics for a specific date
    @Query("SELECT qm FROM QueueMetrics qm WHERE qm.doctor.id = :doctorId AND qm.tenantId = :tenantId AND " +
           "qm.metricDate = :date AND qm.deletedAt IS NULL")
    Optional<QueueMetrics> findByDoctorTenantAndDate(@Param("doctorId") UUID doctorId,
                                                    @Param("tenantId") UUID tenantId,
                                                    @Param("date") LocalDate date);

    // Metrics in date range
    @Query("SELECT qm FROM QueueMetrics qm WHERE qm.doctor.id = :doctorId AND qm.tenantId = :tenantId AND " +
           "qm.metricDate BETWEEN :startDate AND :endDate AND qm.deletedAt IS NULL " +
           "ORDER BY qm.metricDate DESC")
    List<QueueMetrics> findByDoctorTenantAndDateRange(@Param("doctorId") UUID doctorId,
                                                     @Param("tenantId") UUID tenantId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    // Find high-utilization metrics (bottleneck detection)
    @Query("SELECT qm FROM QueueMetrics qm WHERE qm.tenantId = :tenantId AND qm.utilization > 0.85 AND " +
           "qm.metricDate >= :fromDate AND qm.deletedAt IS NULL ORDER BY qm.utilization DESC")
    List<QueueMetrics> findHighUtilizationMetrics(@Param("tenantId") UUID tenantId,
                                                  @Param("fromDate") LocalDate fromDate);

    // Find unstable queues (λ ≥ μ)
    @Query("SELECT qm FROM QueueMetrics qm WHERE qm.tenantId = :tenantId AND " +
           "qm.arrivalRate >= qm.serviceRate AND qm.metricDate >= :fromDate AND qm.deletedAt IS NULL")
    List<QueueMetrics> findUnstableQueues(@Param("tenantId") UUID tenantId,
                                         @Param("fromDate") LocalDate fromDate);

    // Count metrics for a doctor in a date range
    @Query("SELECT COUNT(qm) FROM QueueMetrics qm WHERE qm.doctor.id = :doctorId AND " +
           "qm.tenantId = :tenantId AND qm.metricDate BETWEEN :startDate AND :endDate AND " +
           "qm.deletedAt IS NULL")
    long countByDoctorTenantAndDateRange(@Param("doctorId") UUID doctorId,
                                        @Param("tenantId") UUID tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
