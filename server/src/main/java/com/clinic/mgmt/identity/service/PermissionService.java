package com.clinic.mgmt.identity.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.identity.domain.Permission;
import com.clinic.mgmt.identity.domain.RolePermission;
import com.clinic.mgmt.identity.domain.User;
import com.clinic.mgmt.identity.dto.PermissionRequest;
import com.clinic.mgmt.identity.exception.DuplicatePermissionException;
import com.clinic.mgmt.identity.exception.PermissionNotFoundException;
import com.clinic.mgmt.identity.mapper.PermissionMapper;
import com.clinic.mgmt.identity.repository.PermissionRepository;
import com.clinic.mgmt.identity.repository.RolePermissionRepository;
import com.clinic.mgmt.identity.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PermissionService {

	private static final String SCOPE_TENANT = "tenant";
	private static final String SCOPE_SYSTEM = "system";

	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final UserRepository userRepository;
	private final PermissionMapper permissionMapper;
	private final AuditLogService auditLogService;
	private final MongoTemplate mongoTemplate;

	public PermissionService(
			PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository,
			UserRepository userRepository,
			PermissionMapper permissionMapper,
			AuditLogService auditLogService,
			MongoTemplate mongoTemplate
	) {
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
		this.userRepository = userRepository;
		this.permissionMapper = permissionMapper;
		this.auditLogService = auditLogService;
		this.mongoTemplate = mongoTemplate;
	}

	public List<Permission> list(
			String requestOrganizationId,
			String actorOrganizationId,
			Boolean active,
			String resource,
			String action,
			Boolean includeSystem
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(requestOrganizationId, actorOrganizationId);
		boolean allowSystem = TenantGuard.isSuperAdmin() && Boolean.TRUE.equals(includeSystem);
		if (!allowSystem && SCOPE_SYSTEM.equalsIgnoreCase(resource)) {
			return List.of();
		}

		Criteria criteria = new Criteria();
		if (scopedOrganizationId != null) {
			criteria = criteria.and("organizationId").is(scopedOrganizationId);
		}
		if (!allowSystem) {
			criteria = criteria.and("scope").ne(SCOPE_SYSTEM);
		}
		if (active != null) {
			criteria = criteria.and("active").is(active);
		}
		if (resource != null && !resource.isBlank()) {
			criteria = criteria.and("resource").is(resource);
		}
		if (action != null && !action.isBlank()) {
			criteria = criteria.and("action").is(action);
		}
		Query query = new Query(criteria);
		return mongoTemplate.find(query, Permission.class);
	}

	public Permission get(String id, String organizationId) {
		Permission permission = permissionRepository.findById(id)
				.orElseThrow(() -> new PermissionNotFoundException("Permission not found"));
		if (!TenantGuard.isSuperAdmin() && SCOPE_SYSTEM.equalsIgnoreCase(permission.getScope())) {
			throw new PermissionNotFoundException("Permission not found");
		}
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(permission.getOrganizationId())) {
			throw new PermissionNotFoundException("Permission not found");
		}
		return permission;
	}

	public Permission create(
			PermissionRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		String permissionCode = buildPermissionCode(request.getResource(), request.getAction());
		Optional<Permission> existing = permissionRepository.findByOrganizationIdAndPermissionCode(
				resolvedOrganizationId,
				permissionCode
		);
		if (existing.isPresent()) {
			throw new DuplicatePermissionException("Permission already exists");
		}
		Permission permission = permissionMapper.toNewEntity(request);
		permission.setId(UUID.randomUUID().toString());
		permission.setPermissionCode(permissionCode);
		permission.setScope(normalizeScope(request.getScope(), request.getResource(), null));
		permission.setOrganizationId(resolvedOrganizationId);
		if (permission.getActive() == null) {
			permission.setActive(true);
		}
		Permission saved = permissionRepository.save(permission);
		auditLogService.record(
				actorUserId,
				"create",
				"permissions",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Permission update(
			String id,
			PermissionRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Permission permission = get(id, organizationId);
		if (SCOPE_SYSTEM.equalsIgnoreCase(permission.getScope()) && !TenantGuard.isSuperAdmin()) {
			throw new InvalidRequestException("system scope requires super admin");
		}
		String permissionCode = buildPermissionCode(request.getResource(), request.getAction());
		Optional<Permission> existing = permissionRepository.findByOrganizationIdAndPermissionCode(
				resolvedOrganizationId,
				permissionCode
		);
		if (existing.isPresent() && !existing.get().getId().equals(permission.getId())) {
			throw new DuplicatePermissionException("Permission already exists");
		}
		boolean deactivate = request.getActive() != null && !request.getActive();
		permissionMapper.applyRequest(permission, request);
		permission.setPermissionCode(permissionCode);
		permission.setScope(normalizeScope(request.getScope(), request.getResource(), permission.getScope()));
		permission.setOrganizationId(resolvedOrganizationId);
		Permission saved = permissionRepository.save(permission);
		if (deactivate) {
			rolePermissionRepository.deleteByPermissionId(saved.getId());
		}
		auditLogService.record(
				actorUserId,
				"update",
				"permissions",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void deactivate(String id, String actorUserId, String organizationId, String ipAddress, String userAgent) {
		Permission permission = get(id, organizationId);
		permission.setActive(false);
		permissionRepository.save(permission);
		rolePermissionRepository.deleteByPermissionId(permission.getId());
		auditLogService.record(
				actorUserId,
				"delete",
				"permissions",
				List.of(permission.getId()),
				ipAddress,
				userAgent
		);
	}

	public List<Permission> resolvePermissions(String userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		if (user.getRoleIds() == null || user.getRoleIds().isEmpty()) {
			return List.of();
		}
		List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdIn(user.getRoleIds());
		if (rolePermissions.isEmpty()) {
			return List.of();
		}
		Set<String> permissionIds = rolePermissions.stream()
				.map(RolePermission::getPermissionId)
				.collect(Collectors.toSet());
		if (permissionIds.isEmpty()) {
			return List.of();
		}
		List<Permission> permissions = permissionRepository.findByIdInAndActiveTrue(permissionIds.stream().toList());
		return permissions.stream()
				.sorted(Comparator.comparing(Permission::getPermissionCode, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	public List<String> resolvePermissionCodes(String userId) {
		return resolvePermissions(userId).stream()
				.map(Permission::getPermissionCode)
				.filter(value -> value != null && !value.isBlank())
				.sorted()
				.toList();
	}

	private String buildPermissionCode(String resource, String action) {
		String safeResource = resource == null ? "" : resource.trim();
		String safeAction = action == null ? "" : action.trim();
		return safeResource + "." + safeAction;
	}

	private String normalizeScope(String requestScope, String resource, String currentScope) {
		String base = requestScope == null || requestScope.isBlank() ? currentScope : requestScope;
		String normalized = base == null || base.isBlank() ? SCOPE_TENANT : base.trim().toLowerCase();
		if (SCOPE_SYSTEM.equalsIgnoreCase(resource)) {
			normalized = SCOPE_SYSTEM;
		}
		if (!SCOPE_TENANT.equals(normalized) && !SCOPE_SYSTEM.equals(normalized)) {
			throw new InvalidRequestException("Invalid permission scope");
		}
		if (SCOPE_SYSTEM.equals(normalized) && !TenantGuard.isSuperAdmin()) {
			throw new InvalidRequestException("system scope requires super admin");
		}
		return normalized;
	}
}
