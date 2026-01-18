package com.clinic.mgmt.identity.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.identity.domain.Permission;
import com.clinic.mgmt.identity.domain.Role;
import com.clinic.mgmt.identity.domain.RolePermission;
import com.clinic.mgmt.identity.exception.PermissionNotFoundException;
import com.clinic.mgmt.identity.exception.RoleNotFoundException;
import com.clinic.mgmt.identity.repository.PermissionRepository;
import com.clinic.mgmt.identity.repository.RolePermissionRepository;
import com.clinic.mgmt.identity.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionService {

	private static final String SCOPE_SYSTEM = "system";

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final AuditLogService auditLogService;

	public RolePermissionService(
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository,
			AuditLogService auditLogService
	) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
		this.auditLogService = auditLogService;
	}

	public List<Permission> listPermissions(String roleId, String organizationId) {
		Role role = getRole(roleId, organizationId);
		List<RolePermission> mappings = rolePermissionRepository.findByRoleId(role.getId());
		if (mappings.isEmpty()) {
			return List.of();
		}
		List<String> permissionIds = mappings.stream()
				.map(RolePermission::getPermissionId)
				.distinct()
				.toList();
		List<Permission> permissions = permissionRepository.findAllById(permissionIds);
		if (!TenantGuard.isSuperAdmin()) {
			permissions = permissions.stream()
					.filter(permission -> !SCOPE_SYSTEM.equalsIgnoreCase(permission.getScope()))
					.toList();
		}
		return permissions.stream()
				.sorted(Comparator.comparing(Permission::getPermissionCode, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	public List<Permission> replacePermissions(
			String roleId,
			List<String> permissionIds,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		Role role = getRole(roleId, organizationId);
		Set<String> uniqueIds = new HashSet<>(permissionIds == null ? List.of() : permissionIds);
		List<Permission> permissions = loadPermissions(role, uniqueIds);
		rolePermissionRepository.deleteByRoleId(role.getId());
		if (!uniqueIds.isEmpty()) {
			List<RolePermission> mappings = uniqueIds.stream()
					.map(permissionId -> buildMapping(role, permissionId))
					.toList();
			rolePermissionRepository.saveAll(mappings);
		}
		auditLogService.record(
				actorUserId,
				"update",
				"roles",
				buildTargetIds(role.getId(), uniqueIds),
				ipAddress,
				userAgent
		);
		return permissions;
	}

	public List<Permission> addPermissions(
			String roleId,
			List<String> permissionIds,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		Role role = getRole(roleId, organizationId);
		List<RolePermission> existingMappings = rolePermissionRepository.findByRoleId(role.getId());
		Set<String> existingPermissionIds = existingMappings.stream()
				.map(RolePermission::getPermissionId)
				.collect(Collectors.toSet());
		Set<String> requestedIds = new HashSet<>(permissionIds == null ? List.of() : permissionIds);
		requestedIds.removeAll(existingPermissionIds);
		List<Permission> permissions = loadPermissions(role, requestedIds);
		if (!requestedIds.isEmpty()) {
			List<RolePermission> mappings = requestedIds.stream()
					.map(permissionId -> buildMapping(role, permissionId))
					.toList();
			rolePermissionRepository.saveAll(mappings);
		}
		auditLogService.record(
				actorUserId,
				"update",
				"roles",
				buildTargetIds(role.getId(), requestedIds),
				ipAddress,
				userAgent
		);
		return listPermissions(role.getId(), organizationId);
	}

	public void removePermission(
			String roleId,
			String permissionId,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		Role role = getRole(roleId, organizationId);
		List<RolePermission> mappings = rolePermissionRepository.findByRoleId(role.getId());
		boolean exists = mappings.stream().anyMatch(mapping -> mapping.getPermissionId().equals(permissionId));
		if (!exists) {
			throw new PermissionNotFoundException("Permission not assigned to role");
		}
		rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getId(), permissionId);
		auditLogService.record(
				actorUserId,
				"update",
				"roles",
				List.of(role.getId(), permissionId),
				ipAddress,
				userAgent
		);
	}

	private RolePermission buildMapping(Role role, String permissionId) {
		RolePermission mapping = new RolePermission();
		mapping.setId(UUID.randomUUID().toString());
		mapping.setOrganizationId(role.getOrganizationId());
		mapping.setRoleId(role.getId());
		mapping.setPermissionId(permissionId);
		return mapping;
	}

	private Role getRole(String roleId, String organizationId) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Role role = roleRepository.findById(roleId)
				.orElseThrow(() -> new RoleNotFoundException("Role not found"));
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(role.getOrganizationId())) {
			throw new RoleNotFoundException("Role not found");
		}
		return role;
	}

	private List<Permission> loadPermissions(Role role, Set<String> permissionIds) {
		if (permissionIds == null || permissionIds.isEmpty()) {
			return List.of();
		}
		List<Permission> permissions = permissionRepository.findAllById(permissionIds);
		if (permissions.size() != permissionIds.size()) {
			throw new PermissionNotFoundException("One or more permissions not found");
		}
		boolean orgMismatch = permissions.stream()
				.anyMatch(permission -> !role.getOrganizationId().equals(permission.getOrganizationId()));
		if (orgMismatch) {
			throw new PermissionNotFoundException("Permission organization mismatch");
		}
		if (!TenantGuard.isSuperAdmin()
				&& permissions.stream().anyMatch(permission -> SCOPE_SYSTEM.equalsIgnoreCase(permission.getScope()))) {
			throw new PermissionNotFoundException("Permission not found");
		}
		return permissions.stream()
				.sorted(Comparator.comparing(Permission::getPermissionCode, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	private List<String> buildTargetIds(String roleId, Set<String> permissionIds) {
		List<String> targets = new java.util.ArrayList<>();
		targets.add(roleId);
		if (permissionIds != null) {
			targets.addAll(permissionIds);
		}
		return targets;
	}
}
