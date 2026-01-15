package com.clinic.backend.repository;

import com.clinic.common.entity.core.User;
import com.clinic.common.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Uniqueness enforcement per tenant (Injective function within tenant scope)
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<User> findByPhoneAndTenantId(String phone, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByPhoneAndTenantId(String phone, UUID tenantId);

    // Tenant-scoped queries
    List<User> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    List<User> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, UserStatus status);

    // Status queries
    List<User> findByStatusAndDeletedAtIsNull(UserStatus status);

    // Authentication queries
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndTenantIdForAuthentication(@Param("email") String email, @Param("tenantId") UUID tenantId);

    // Locked accounts
    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil > :now AND u.deletedAt IS NULL")
    List<User> findLockedAccounts(@Param("now") Instant now);

    // Failed login attempts
    @Query("SELECT u FROM User u WHERE u.loginAttempts >= :maxAttempts AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    List<User> findUsersWithExcessiveLoginAttempts(@Param("maxAttempts") Integer maxAttempts, @Param("tenantId") UUID tenantId);

    // Password expiry
    @Query("SELECT u FROM User u WHERE u.passwordChangedAt < :expiryDate AND u.status = :status AND u.deletedAt IS NULL")
    List<User> findUsersWithExpiredPasswords(@Param("expiryDate") Instant expiryDate, @Param("status") UserStatus status);

    // Soft delete support
    Optional<User> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Counting for capacity constraints
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
