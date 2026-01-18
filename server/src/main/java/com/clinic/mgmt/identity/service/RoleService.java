package com.clinic.mgmt.identity.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.identity.domain.Role;
import com.clinic.mgmt.identity.dto.RoleRequest;
import com.clinic.mgmt.identity.exception.DuplicateRoleException;
import com.clinic.mgmt.identity.exception.RoleInUseException;
import com.clinic.mgmt.identity.exception.RoleNotFoundException;
import com.clinic.mgmt.identity.mapper.RoleMapper;
import com.clinic.mgmt.identity.repository.RolePermissionRepository;
import com.clinic.mgmt.identity.repository.RoleRepository;
import com.clinic.mgmt.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

	private final RoleRepository roleRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final UserRepository userRepository;
	private final RoleMapper roleMapper;
	private final AuditLogService auditLogService;

	public RoleService(
			RoleRepository roleRepository,
			RolePermissionRepository rolePermissionRepository,
			UserRepository userRepository,
			RoleMapper roleMapper,
			AuditLogService auditLogService
	) {
		this.roleRepository = roleRepository;
		this.rolePermissionRepository = rolePermissionRepository;
		this.userRepository = userRepository;
		this.roleMapper = roleMapper;
		this.auditLogService = auditLogService;
	}

	public List<Role> list(String requestOrganizationId, String actorOrganizationId) {
		String scopedOrganizationId = TenantGuard.resolveScope(requestOrganizationId, actorOrganizationId);
		if (scopedOrganizationId == null) {
			return roleRepository.findAll();
		}
		return roleRepository.findByOrganizationId(scopedOrganizationId);
	}

	public Role get(String id, String organizationId) {
		Role role = roleRepository.findById(id)
				.orElseThrow(() -> new RoleNotFoundException("Role not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(role.getOrganizationId())) {
			throw new RoleNotFoundException("Role not found");
		}
		return role;
	}

	public Role create(
			RoleRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		ensureUnique(request.getOrganizationId(), request.getName(), request.getRoleCode(), null);
		Role role = roleMapper.toNewEntity(request);
		role.setId(UUID.randomUUID().toString());
		role.setOrganizationId(resolvedOrganizationId);
		Role saved = roleRepository.save(role);
		auditLogService.record(
				actorUserId,
				"create",
				"roles",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Role update(
			String id,
			RoleRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Role role = get(id, organizationId);
		if (!role.getOrganizationId().equals(request.getOrganizationId())) {
			throw new InvalidRequestException("organization_id cannot be changed for roles");
		}
		ensureUnique(request.getOrganizationId(), request.getName(), request.getRoleCode(), role.getId());
		roleMapper.applyRequest(role, request);
		role.setOrganizationId(resolvedOrganizationId);
		Role saved = roleRepository.save(role);
		auditLogService.record(
				actorUserId,
				"update",
				"roles",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void delete(String id, String actorUserId, String organizationId, String ipAddress, String userAgent) {
		Role role = get(id, organizationId);
		if (rolePermissionRepository.existsByRoleId(role.getId())) {
			throw new RoleInUseException("Role has assigned permissions");
		}
		if (userRepository.existsByRoleIdsContains(role.getId())) {
			throw new RoleInUseException("Role is assigned to users");
		}
		roleRepository.delete(role);
		auditLogService.record(
				actorUserId,
				"delete",
				"roles",
				List.of(role.getId()),
				ipAddress,
				userAgent
		);
	}

	private void ensureUnique(String organizationId, String name, String roleCode, String existingId) {
		Optional<Role> byCode = roleRepository.findByOrganizationIdAndRoleCode(organizationId, roleCode);
		if (byCode.isPresent() && !byCode.get().getId().equals(existingId)) {
			throw new DuplicateRoleException("Role code already exists");
		}
		Optional<Role> byName = roleRepository.findByOrganizationIdAndName(organizationId, name);
		if (byName.isPresent() && !byName.get().getId().equals(existingId)) {
			throw new DuplicateRoleException("Role name already exists");
		}
	}
}
