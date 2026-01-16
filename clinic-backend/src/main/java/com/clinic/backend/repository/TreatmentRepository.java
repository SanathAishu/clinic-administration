package com.clinic.backend.repository;

import com.clinic.common.entity.operational.Treatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {

    // Tenant-scoped queries
    Page<Treatment> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Treatment> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Search by name
    @Query("SELECT t FROM Treatment t WHERE t.tenantId = :tenantId AND " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) AND t.deletedAt IS NULL")
    Page<Treatment> searchByName(@Param("tenantId") UUID tenantId,
                                  @Param("name") String name,
                                  Pageable pageable);

    // Find by category
    List<Treatment> findByTenantIdAndCategoryAndDeletedAtIsNull(UUID tenantId, String category);

    // Find active treatments
    List<Treatment> findByTenantIdAndIsActiveAndDeletedAtIsNull(UUID tenantId, Boolean isActive);

    // Find active treatments by category
    @Query("SELECT t FROM Treatment t WHERE t.tenantId = :tenantId AND " +
           "t.category = :category AND t.isActive = true AND t.deletedAt IS NULL")
    List<Treatment> findActiveTreatmentsByCategory(@Param("tenantId") UUID tenantId,
                                                     @Param("category") String category);

    // Get all categories for a tenant
    @Query("SELECT DISTINCT t.category FROM Treatment t WHERE t.tenantId = :tenantId AND " +
           "t.category IS NOT NULL AND t.deletedAt IS NULL ORDER BY t.category")
    List<String> findAllCategories(@Param("tenantId") UUID tenantId);

    // Count treatments
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndIsActiveAndDeletedAtIsNull(UUID tenantId, Boolean isActive);

    // Check if treatment name exists
    boolean existsByNameAndTenantIdAndDeletedAtIsNull(String name, UUID tenantId);
}
