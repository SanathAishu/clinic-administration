package com.clinic.backend.repository;

import com.clinic.common.dto.view.UserDetailViewDTO;
import com.clinic.common.dto.view.UserListViewDTO;
import com.clinic.common.enums.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CQRS READ operations on User domain.
 * Uses native SQL queries against database views for optimized reads.
 *
 * CQRS Pattern:
 * - READ: This repository uses database views (v_user_list, v_user_detail)
 * - WRITE: Use UserRepository with JPA entities
 */
@Repository
public class UserViewRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * List all users for a tenant (non-deleted).
     * Uses v_user_list view for pre-joined role information.
     */
    public List<UserListViewDTO> findAllByTenantId(UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, first_name, last_name, full_name, email, phone,
                   status, last_login_at, login_attempts, locked_until,
                   role_names, roles, role_count, is_active, is_locked, is_deleted,
                   created_at, updated_at
            FROM v_user_list
            WHERE tenant_id = ? AND is_deleted = false
            ORDER BY full_name
            """;

        return jdbcTemplate.query(sql, new UserListViewRowMapper(), tenantId);
    }

    /**
     * Get user detail by ID with full role/permission hierarchy.
     * Uses v_user_detail view for complete user profile.
     */
    public Optional<UserDetailViewDTO> findDetailById(UUID id, UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, first_name, last_name, full_name, email, phone,
                   status, last_login_at, login_attempts, locked_until, password_changed_at,
                   tenant_name, tenant_subdomain, roles_with_permissions, permission_names,
                   created_at, updated_at, deleted_at
            FROM v_user_detail
            WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL
            """;

        List<UserDetailViewDTO> results = jdbcTemplate.query(sql, new UserDetailViewRowMapper(), id, tenantId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * List doctors only (users with DOCTOR role).
     * Filters by role name in the aggregated role_names column.
     */
    public List<UserListViewDTO> findDoctorsByTenantId(UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, first_name, last_name, full_name, email, phone,
                   status, last_login_at, login_attempts, locked_until,
                   role_names, roles, role_count, is_active, is_locked, is_deleted,
                   created_at, updated_at
            FROM v_user_list
            WHERE tenant_id = ? AND is_deleted = false
              AND (role_names ILIKE '%DOCTOR%' OR role_names ILIKE '%Doctor%')
            ORDER BY full_name
            """;

        return jdbcTemplate.query(sql, new UserListViewRowMapper(), tenantId);
    }

    /**
     * Search users by name or email.
     * Case-insensitive partial match on full_name and email.
     */
    public List<UserListViewDTO> searchByTenantId(UUID tenantId, String searchTerm) {
        String sql = """
            SELECT id, tenant_id, first_name, last_name, full_name, email, phone,
                   status, last_login_at, login_attempts, locked_until,
                   role_names, roles, role_count, is_active, is_locked, is_deleted,
                   created_at, updated_at
            FROM v_user_list
            WHERE tenant_id = ? AND is_deleted = false
              AND (full_name ILIKE ? OR email ILIKE ?)
            ORDER BY full_name
            """;

        String pattern = "%" + searchTerm + "%";
        return jdbcTemplate.query(sql, new UserListViewRowMapper(), tenantId, pattern, pattern);
    }

    /**
     * List users by status for a tenant.
     */
    public List<UserListViewDTO> findByTenantIdAndStatus(UUID tenantId, UserStatus status) {
        String sql = """
            SELECT id, tenant_id, first_name, last_name, full_name, email, phone,
                   status, last_login_at, login_attempts, locked_until,
                   role_names, roles, role_count, is_active, is_locked, is_deleted,
                   created_at, updated_at
            FROM v_user_list
            WHERE tenant_id = ? AND status = ? AND is_deleted = false
            ORDER BY full_name
            """;

        return jdbcTemplate.query(sql, new UserListViewRowMapper(), tenantId, status.name());
    }

    /**
     * List locked users for a tenant.
     */
    public List<UserListViewDTO> findLockedByTenantId(UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, first_name, last_name, full_name, email, phone,
                   status, last_login_at, login_attempts, locked_until,
                   role_names, roles, role_count, is_active, is_locked, is_deleted,
                   created_at, updated_at
            FROM v_user_list
            WHERE tenant_id = ? AND is_locked = true AND is_deleted = false
            ORDER BY full_name
            """;

        return jdbcTemplate.query(sql, new UserListViewRowMapper(), tenantId);
    }

    /**
     * RowMapper for UserListViewDTO
     */
    private static class UserListViewRowMapper implements RowMapper<UserListViewDTO> {
        @Override
        public UserListViewDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return UserListViewDTO.builder()
                    .id(getUUID(rs, "id"))
                    .tenantId(getUUID(rs, "tenant_id"))
                    .firstName(rs.getString("first_name"))
                    .lastName(rs.getString("last_name"))
                    .fullName(rs.getString("full_name"))
                    .email(rs.getString("email"))
                    .phone(rs.getString("phone"))
                    .status(getUserStatus(rs, "status"))
                    .lastLoginAt(getInstant(rs, "last_login_at"))
                    .loginAttempts(rs.getInt("login_attempts"))
                    .lockedUntil(getInstant(rs, "locked_until"))
                    .roleNames(rs.getString("role_names"))
                    .roles(rs.getString("roles"))
                    .roleCount(rs.getInt("role_count"))
                    .isActive(rs.getBoolean("is_active"))
                    .isLocked(rs.getBoolean("is_locked"))
                    .isDeleted(rs.getBoolean("is_deleted"))
                    .createdAt(getInstant(rs, "created_at"))
                    .updatedAt(getInstant(rs, "updated_at"))
                    .build();
        }
    }

    /**
     * RowMapper for UserDetailViewDTO
     */
    private static class UserDetailViewRowMapper implements RowMapper<UserDetailViewDTO> {
        @Override
        public UserDetailViewDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return UserDetailViewDTO.builder()
                    .id(getUUID(rs, "id"))
                    .tenantId(getUUID(rs, "tenant_id"))
                    .firstName(rs.getString("first_name"))
                    .lastName(rs.getString("last_name"))
                    .fullName(rs.getString("full_name"))
                    .email(rs.getString("email"))
                    .phone(rs.getString("phone"))
                    .status(getUserStatus(rs, "status"))
                    .lastLoginAt(getInstant(rs, "last_login_at"))
                    .loginAttempts(rs.getInt("login_attempts"))
                    .lockedUntil(getInstant(rs, "locked_until"))
                    .passwordChangedAt(getInstant(rs, "password_changed_at"))
                    .tenantName(rs.getString("tenant_name"))
                    .tenantSubdomain(rs.getString("tenant_subdomain"))
                    .rolesWithPermissions(rs.getString("roles_with_permissions"))
                    .permissionNames(rs.getString("permission_names"))
                    .createdAt(getInstant(rs, "created_at"))
                    .updatedAt(getInstant(rs, "updated_at"))
                    .deletedAt(getInstant(rs, "deleted_at"))
                    .build();
        }
    }

    // Helper methods for null-safe type conversions
    private static UUID getUUID(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value != null ? UUID.fromString(value) : null;
    }

    private static Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }

    private static UserStatus getUserStatus(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value != null ? UserStatus.valueOf(value) : null;
    }
}
