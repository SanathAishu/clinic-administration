package com.clinic.backend.service;

import com.clinic.common.entity.core.Tenant;
import com.clinic.common.entity.core.User;
import com.clinic.backend.repository.UserRepository;
import com.clinic.common.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    private static final int PASSWORD_EXPIRY_DAYS = 90;

    /**
     * Create new user (Injective function within tenant: (email, tenantId) → user)
     */
    @Transactional
    public User createUser(User user, UUID tenantId) {
        log.debug("Creating user: {} for tenant: {}", user.getEmail(), tenantId);

        // Validate tenant and capacity (Cardinality constraint)
        Tenant tenant = tenantService.getTenantById(tenantId);
        long currentUserCount = userRepository.countByTenantIdAndDeletedAtIsNull(tenantId);

        if (currentUserCount >= tenant.getMaxUsers()) {
            throw new IllegalStateException(
                    String.format("Tenant user limit reached: %d/%d", currentUserCount, tenant.getMaxUsers()));
        }

        // Uniqueness validation (Set Theory)
        if (userRepository.existsByEmailAndTenantId(user.getEmail(), tenantId)) {
            throw new IllegalArgumentException("Email already exists in this tenant: " + user.getEmail());
        }

        if (user.getPhone() != null && userRepository.existsByPhoneAndTenantId(user.getPhone(), tenantId)) {
            throw new IllegalArgumentException("Phone already exists in this tenant: " + user.getPhone());
        }

        // Set tenant
        user.setTenantId(tenantId);

        // Hash password
        if (user.getPasswordHash() != null) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }

        // Set defaults
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }

        if (user.getLoginAttempts() == null) {
            user.setLoginAttempts(0);
        }

        user.setPasswordChangedAt(Instant.now());

        User saved = userRepository.save(user);
        log.info("Created user: {} with ID: {}", saved.getEmail(), saved.getId());
        return saved;
    }

    /**
     * Get user by ID (tenant-scoped)
     */
    public User getUserById(UUID id, UUID tenantId) {
        return userRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    /**
     * Get user by email (Injective lookup)
     */
    public User getUserByEmail(String email, UUID tenantId) {
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    /**
     * Get all users for tenant
     */
    public List<User> getAllUsersForTenant(UUID tenantId) {
        return userRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    /**
     * Get users by status
     */
    public List<User> getUsersByStatus(UUID tenantId, UserStatus status) {
        return userRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    /**
     * Update user
     */
    @Transactional
    public User updateUser(UUID id, UUID tenantId, User updates) {
        User user = getUserById(id, tenantId);

        // Update allowed fields
        if (updates.getFirstName() != null) {
            user.setFirstName(updates.getFirstName());
        }

        if (updates.getLastName() != null) {
            user.setLastName(updates.getLastName());
        }

        if (updates.getEmail() != null && !updates.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndTenantId(updates.getEmail(), tenantId)) {
                throw new IllegalArgumentException("Email already exists: " + updates.getEmail());
            }
            user.setEmail(updates.getEmail());
        }

        if (updates.getPhone() != null && !updates.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhoneAndTenantId(updates.getPhone(), tenantId)) {
                throw new IllegalArgumentException("Phone already exists: " + updates.getPhone());
            }
            user.setPhone(updates.getPhone());
        }

        User saved = userRepository.save(user);
        log.info("Updated user: {}", saved.getId());
        return saved;
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(UUID userId, UUID tenantId, String oldPassword, String newPassword) {
        User user = getUserById(userId, tenantId);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        // Hash and set new password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());

        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }

    /**
     * Reset password (admin operation)
     */
    @Transactional
    public void resetPassword(UUID userId, UUID tenantId, String newPassword) {
        User user = getUserById(userId, tenantId);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);
        log.info("Password reset for user: {}", userId);
    }

    /**
     * Handle failed login attempt (Boolean Logic)
     */
    @Transactional
    public void handleFailedLogin(UUID userId, UUID tenantId) {
        User user = getUserById(userId, tenantId);

        user.incrementLoginAttempts();

        // Lock account if max attempts exceeded (Cardinality threshold)
        if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.lock(ACCOUNT_LOCK_DURATION_MINUTES);
            log.warn("Account locked due to excessive login attempts: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    /**
     * Handle successful login (Idempotent reset)
     */
    @Transactional
    public void handleSuccessfulLogin(UUID userId, UUID tenantId) {
        User user = getUserById(userId, tenantId);

        user.resetLoginAttempts();
        user.setLastLoginAt(Instant.now());

        userRepository.save(user);
        log.debug("Successful login for user: {}", user.getEmail());
    }

    /**
     * Unlock user account (Idempotent operation)
     */
    @Transactional
    public void unlockAccount(UUID userId, UUID tenantId) {
        User user = getUserById(userId, tenantId);

        user.resetLoginAttempts();
        user.setLockedUntil(null);

        userRepository.save(user);
        log.info("Unlocked account: {}", user.getEmail());
    }

    /**
     * Update user status (State machine)
     */
    @Transactional
    public User updateUserStatus(UUID userId, UUID tenantId, UserStatus newStatus) {
        User user = getUserById(userId, tenantId);

        // Validate state transition
        validateStatusTransition(user.getStatus(), newStatus);

        user.setStatus(newStatus);

        User saved = userRepository.save(user);
        log.info("Updated user status: {} -> {}", saved.getId(), newStatus);
        return saved;
    }

    /**
     * Get locked accounts
     */
    public List<User> getLockedAccounts() {
        return userRepository.findLockedAccounts(Instant.now());
    }

    /**
     * Get users with expired passwords
     */
    public List<User> getUsersWithExpiredPasswords(UUID tenantId) {
        Instant expiryDate = Instant.now().minus(PASSWORD_EXPIRY_DAYS, ChronoUnit.DAYS);
        return userRepository.findUsersWithExpiredPasswords(expiryDate, UserStatus.ACTIVE);
    }

    /**
     * Check if user password is expired
     */
    public boolean isPasswordExpired(User user) {
        if (user.getPasswordChangedAt() == null) {
            return true;
        }
        Instant expiryDate = user.getPasswordChangedAt().plus(PASSWORD_EXPIRY_DAYS, ChronoUnit.DAYS);
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Soft delete user (Idempotent)
     */
    @Transactional
    public void softDeleteUser(UUID userId, UUID tenantId) {
        User user = getUserById(userId, tenantId);
        user.softDelete();
        userRepository.save(user);
        log.info("Soft deleted user: {}", userId);
    }

    /**
     * Validate state transition (State machine validation)
     */
    private void validateStatusTransition(UserStatus current, UserStatus next) {
        // Valid transitions (DAG)
        boolean valid = switch (current) {
            case ACTIVE -> next == UserStatus.INACTIVE || next == UserStatus.SUSPENDED || next == UserStatus.LOCKED;
            case INACTIVE -> next == UserStatus.ACTIVE || next == UserStatus.SUSPENDED;
            case SUSPENDED -> next == UserStatus.ACTIVE || next == UserStatus.INACTIVE;
            case LOCKED -> next == UserStatus.ACTIVE || next == UserStatus.SUSPENDED;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s -> %s", current, next));
        }
    }
}
