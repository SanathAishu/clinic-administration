package com.clinic.backend.repository;

import com.clinic.common.dto.view.RolePermissionsViewDTO;
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
 * Repository for CQRS READ operations on Role domain.
 * Uses native SQL queries against database views for optimized reads.
 *
 * CQRS Pattern:
 * - READ: This repository uses database views (v_role_permissions)
 * - WRITE: Use RoleRepository with JPA entities
 */
@Repository
public class RoleViewRepository {

    private final JdbcTemplate jdbcTemplate;

    public RoleViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * List all roles for a tenant with permissions as JSON.
     * Uses v_role_permissions view for pre-aggregated permission data.
     */
    public List<RolePermissionsViewDTO> findAllByTenantId(UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, name, description, is_system_role,
                   permissions, permission_count, user_count, created_at, updated_at
            FROM v_role_permissions
            WHERE tenant_id = ? OR is_system_role = true
            ORDER BY is_system_role DESC, name
            """;

        return jdbcTemplate.query(sql, new RolePermissionsViewRowMapper(), tenantId);
    }

    /**
     * Get role detail by ID with permissions.
     */
    public Optional<RolePermissionsViewDTO> findDetailById(UUID id, UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, name, description, is_system_role,
                   permissions, permission_count, user_count, created_at, updated_at
            FROM v_role_permissions
            WHERE id = ? AND (tenant_id = ? OR is_system_role = true)
            """;

        List<RolePermissionsViewDTO> results = jdbcTemplate.query(sql, new RolePermissionsViewRowMapper(), id, tenantId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Get system roles only.
     */
    public List<RolePermissionsViewDTO> findSystemRoles() {
        String sql = """
            SELECT id, tenant_id, name, description, is_system_role,
                   permissions, permission_count, user_count, created_at, updated_at
            FROM v_role_permissions
            WHERE is_system_role = true
            ORDER BY name
            """;

        return jdbcTemplate.query(sql, new RolePermissionsViewRowMapper());
    }

    /**
     * Get tenant-specific roles only (excluding system roles).
     */
    public List<RolePermissionsViewDTO> findTenantRoles(UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, name, description, is_system_role,
                   permissions, permission_count, user_count, created_at, updated_at
            FROM v_role_permissions
            WHERE tenant_id = ? AND is_system_role = false
            ORDER BY name
            """;

        return jdbcTemplate.query(sql, new RolePermissionsViewRowMapper(), tenantId);
    }

    /**
     * Search roles by name.
     */
    public List<RolePermissionsViewDTO> searchByTenantId(UUID tenantId, String searchTerm) {
        String sql = """
            SELECT id, tenant_id, name, description, is_system_role,
                   permissions, permission_count, user_count, created_at, updated_at
            FROM v_role_permissions
            WHERE (tenant_id = ? OR is_system_role = true)
              AND (name ILIKE ? OR description ILIKE ?)
            ORDER BY is_system_role DESC, name
            """;

        String pattern = "%" + searchTerm + "%";
        return jdbcTemplate.query(sql, new RolePermissionsViewRowMapper(), tenantId, pattern, pattern);
    }

    /**
     * RowMapper for RolePermissionsViewDTO
     */
    private static class RolePermissionsViewRowMapper implements RowMapper<RolePermissionsViewDTO> {
        @Override
        public RolePermissionsViewDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return RolePermissionsViewDTO.builder()
                    .id(getUUID(rs, "id"))
                    .tenantId(getUUID(rs, "tenant_id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .isSystemRole(rs.getBoolean("is_system_role"))
                    .permissions(rs.getString("permissions"))
                    .permissionCount(rs.getInt("permission_count"))
                    .userCount(rs.getInt("user_count"))
                    .createdAt(getInstant(rs, "created_at"))
                    .updatedAt(getInstant(rs, "updated_at"))
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
}
