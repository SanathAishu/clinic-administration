package com.clinic.mgmt.identity.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.identity.domain.Role;
import com.clinic.mgmt.identity.domain.User;
import com.clinic.mgmt.identity.dto.UserCreateRequest;
import com.clinic.mgmt.identity.dto.UserRoleUpdateRequest;
import com.clinic.mgmt.identity.dto.UserUpdateRequest;
import com.clinic.mgmt.identity.exception.DuplicateUserException;
import com.clinic.mgmt.identity.exception.InvalidRoleAssignmentException;
import com.clinic.mgmt.identity.exception.UserNotFoundException;
import com.clinic.mgmt.identity.mapper.UserMapper;
import com.clinic.mgmt.identity.repository.RoleRepository;
import com.clinic.mgmt.identity.repository.UserRepository;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private static final Set<String> VALID_STATUSES = Set.of("active", "inactive");

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final MongoTemplate mongoTemplate;

	public UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserMapper userMapper,
			PasswordEncoder passwordEncoder,
			AuditLogService auditLogService,
			MongoTemplate mongoTemplate
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.mongoTemplate = mongoTemplate;
	}

	public List<User> list(String organizationId, String status, String roleCode, String clinicId, String branchId) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organizationId").is(scopedOrganizationId));
		}
		if (status != null && !status.isBlank()) {
			query.addCriteria(Criteria.where("status").is(status.toLowerCase()));
		}
		if (roleCode != null && !roleCode.isBlank()) {
			query.addCriteria(Criteria.where("roleCodes").is(roleCode));
		}
		if ((clinicId != null && !clinicId.isBlank()) || (branchId != null && !branchId.isBlank())) {
			List<String> userIds = resolveUserIdsByStaff(clinicId, branchId);
			if (userIds.isEmpty()) {
				return List.of();
			}
			query.addCriteria(Criteria.where("_id").in(userIds));
		}
		return mongoTemplate.find(query, User.class);
	}

	public User get(String id, String organizationId) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(user.getOrganizationId())) {
			throw new UserNotFoundException("User not found");
		}
		return user;
	}

	public User create(
			UserCreateRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		validateContact(request.getEmail(), request.getPhone());
		ensureUnique(request.getEmail(), request.getPhone(), null);
		User user = userMapper.toNewEntity(request);
		user.setId(UUID.randomUUID().toString());
		user.setOrganizationId(resolvedOrganizationId);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setStatus(normalizeStatus(request.getStatus()));
		RoleAssignment assignment = resolveRoles(
				request.getOrganizationId(),
				request.getRoleIds(),
				request.getRoleCodes()
		);
		user.setRoleIds(assignment.roleIds());
		user.setRoleCodes(assignment.roleCodes());
		User saved = userRepository.save(user);
		auditLogService.record(
				actorUserId,
				"create",
				"users",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public User update(
			String id,
			UserUpdateRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		User user = get(id, organizationId);
		if (request.getEmail() != null && !Objects.equals(user.getEmail(), request.getEmail())) {
			ensureUnique(request.getEmail(), null, user.getId());
		}
		if (request.getPhone() != null && !Objects.equals(user.getPhone(), request.getPhone())) {
			ensureUnique(null, request.getPhone(), user.getId());
		}
		userMapper.applyUpdate(user, request);
		if (request.getStatus() != null) {
			user.setStatus(normalizeStatus(request.getStatus()));
		}
		User saved = userRepository.save(user);
		auditLogService.record(
				actorUserId,
				"update",
				"users",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public User updateRoles(
			String id,
			UserRoleUpdateRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		User user = get(id, organizationId);
		RoleAssignment assignment = resolveRoles(user.getOrganizationId(), request.getRoleIds(), request.getRoleCodes());
		user.setRoleIds(assignment.roleIds());
		user.setRoleCodes(assignment.roleCodes());
		User saved = userRepository.save(user);
		auditLogService.record(
				actorUserId,
				"update",
				"users",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public User updateStatus(
			String id,
			String status,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		User user = get(id, organizationId);
		user.setStatus(normalizeStatus(status));
		User saved = userRepository.save(user);
		auditLogService.record(
				actorUserId,
				"update",
				"users",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void deactivate(String id, String actorUserId, String organizationId, String ipAddress, String userAgent) {
		User user = get(id, organizationId);
		user.setStatus("inactive");
		userRepository.save(user);
		auditLogService.record(
				actorUserId,
				"delete",
				"users",
				List.of(user.getId()),
				ipAddress,
				userAgent
		);
	}

	private void validateContact(String email, String phone) {
		if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or phone is required");
		}
	}

	private void ensureUnique(String email, String phone, String existingId) {
		if (email != null && !email.isBlank()) {
			userRepository.findByEmailIgnoreCase(email)
					.filter(user -> !user.getId().equals(existingId))
					.ifPresent(user -> {
						throw new DuplicateUserException("Email already exists");
					});
		}
		if (phone != null && !phone.isBlank()) {
			userRepository.findByPhone(phone)
					.filter(user -> !user.getId().equals(existingId))
					.ifPresent(user -> {
						throw new DuplicateUserException("Phone already exists");
					});
		}
	}

	private String normalizeStatus(String status) {
		String normalized = status == null || status.isBlank() ? "active" : status.toLowerCase();
		if (!VALID_STATUSES.contains(normalized)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
		}
		return normalized;
	}

	private RoleAssignment resolveRoles(String organizationId, List<String> roleIds, List<String> roleCodes) {
		boolean hasIds = roleIds != null && !roleIds.isEmpty();
		boolean hasCodes = roleCodes != null && !roleCodes.isEmpty();
		if (!hasIds && !hasCodes) {
			return new RoleAssignment(List.of(), List.of());
		}

		List<Role> rolesById = hasIds ? roleRepository.findByIdIn(roleIds) : List.of();
		if (hasIds && rolesById.size() != roleIds.size()) {
			throw new InvalidRoleAssignmentException("One or more role ids are invalid");
		}
		List<Role> rolesByCode = hasCodes
				? roleRepository.findByOrganizationIdAndRoleCodeIn(organizationId, roleCodes)
				: List.of();
		if (hasCodes && rolesByCode.size() != roleCodes.size()) {
			throw new InvalidRoleAssignmentException("One or more role codes are invalid");
		}

		if (hasIds && rolesById.stream().anyMatch(role -> !organizationId.equals(role.getOrganizationId()))) {
			throw new InvalidRoleAssignmentException("Role organization mismatch");
		}

		if (hasIds && hasCodes) {
			Set<String> idSet = new HashSet<>(roleIds);
			Set<String> idsFromCodes = rolesByCode.stream().map(Role::getId).collect(Collectors.toSet());
			if (!idSet.equals(idsFromCodes)) {
				throw new InvalidRoleAssignmentException("Role ids and role codes do not match");
			}
		}

		List<Role> resolved = hasIds ? rolesById : rolesByCode;
		List<String> resolvedIds = resolved.stream().map(Role::getId).toList();
		List<String> resolvedCodes = resolved.stream().map(Role::getRoleCode).toList();
		return new RoleAssignment(resolvedIds, resolvedCodes);
	}

	private List<String> resolveUserIdsByStaff(String clinicId, String branchId) {
		Query staffQuery = new Query();
		if (clinicId != null && !clinicId.isBlank()) {
			staffQuery.addCriteria(Criteria.where("clinic_id").is(clinicId));
		}
		if (branchId != null && !branchId.isBlank()) {
			staffQuery.addCriteria(Criteria.where("branch_id").is(branchId));
		}
		List<Document> staffDocs = mongoTemplate.find(staffQuery, Document.class, "staff");
		return staffDocs.stream()
				.map(doc -> doc.getString("user_id"))
				.filter(Objects::nonNull)
				.toList();
	}

	private record RoleAssignment(List<String> roleIds, List<String> roleCodes) {
	}
}
