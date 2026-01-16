package com.clinic.backend.repository;

import com.clinic.common.entity.core.Branch;
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
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    // Tenant-scoped queries
    List<Branch> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Page<Branch> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Branch> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Find by branch code
    Optional<Branch> findByBranchCodeAndTenantIdAndDeletedAtIsNull(String branchCode, UUID tenantId);

    // Find active branches
    List<Branch> findByTenantIdAndIsActiveAndDeletedAtIsNull(UUID tenantId, Boolean isActive);

    // Find main branch
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND " +
           "b.isMainBranch = true AND b.deletedAt IS NULL")
    Optional<Branch> findMainBranch(@Param("tenantId") UUID tenantId);

    // Search by name or city
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.city) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "b.deletedAt IS NULL")
    Page<Branch> searchBranches(@Param("tenantId") UUID tenantId,
                                 @Param("search") String search,
                                 Pageable pageable);

    // Count branches
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndIsActiveAndDeletedAtIsNull(UUID tenantId, Boolean isActive);

    // Check uniqueness
    boolean existsByBranchCodeAndTenantIdAndDeletedAtIsNull(String branchCode, UUID tenantId);

    boolean existsByNameAndTenantIdAndDeletedAtIsNull(String name, UUID tenantId);
}
