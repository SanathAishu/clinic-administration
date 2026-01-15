package com.clinic.backend.repository;

import com.clinic.common.entity.core.Tenant;
import com.clinic.common.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // Uniqueness enforcement (Set Theory - Injective function)
    Optional<Tenant> findBySubdomain(String subdomain);

    Optional<Tenant> findByEmail(String email);

    boolean existsBySubdomain(String subdomain);

    boolean existsByEmail(String email);

    // Status-based queries
    List<Tenant> findByStatus(TenantStatus status);

    List<Tenant> findByStatusAndDeletedAtIsNull(TenantStatus status);

    // Subscription queries
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndDate < :date AND t.status = :status AND t.deletedAt IS NULL")
    List<Tenant> findExpiredSubscriptions(@Param("date") LocalDate date, @Param("status") TenantStatus status);

    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndDate BETWEEN :startDate AND :endDate AND t.deletedAt IS NULL")
    List<Tenant> findExpiringSubscriptions(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Capacity queries (Combinatorics - Cardinality constraints)
    @Query("SELECT t FROM Tenant t WHERE t.deletedAt IS NULL ORDER BY t.createdAt DESC")
    List<Tenant> findAllActive();

    // Soft delete support
    @Query("SELECT t FROM Tenant t WHERE t.deletedAt IS NULL")
    List<Tenant> findAllNonDeleted();

    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Tenant> findBySubdomainAndDeletedAtIsNull(String subdomain);
}
