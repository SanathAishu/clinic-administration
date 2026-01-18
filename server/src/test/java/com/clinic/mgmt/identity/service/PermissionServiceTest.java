package com.clinic.mgmt.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.identity.domain.Permission;
import com.clinic.mgmt.identity.mapper.PermissionMapper;
import com.clinic.mgmt.identity.repository.PermissionRepository;
import com.clinic.mgmt.identity.repository.RolePermissionRepository;
import com.clinic.mgmt.identity.repository.UserRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private RolePermissionRepository rolePermissionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PermissionMapper permissionMapper;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private MongoTemplate mongoTemplate;

	@InjectMocks
	private PermissionService permissionService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void list_returnsEmptyWhenSystemResourceNotAllowed() {
		setPermissions(List.of());

		List<Permission> results = permissionService.list(
				null,
				"org-alpha",
				null,
				"system",
				null,
				true
		);

		assertTrue(results.isEmpty());
		verifyNoInteractions(mongoTemplate);
	}

	@Test
	void list_addsScopeFilterWhenIncludeSystemFalse() {
		setPermissions(List.of("system.super_admin"));
		when(mongoTemplate.find(any(Query.class), eq(Permission.class))).thenReturn(List.of());

		permissionService.list(
				null,
				"org-alpha",
				null,
				null,
				null,
				false
		);

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate).find(queryCaptor.capture(), eq(Permission.class));
		Document queryObject = queryCaptor.getValue().getQueryObject();
		Object scope = queryObject.get("scope");
		assertTrue(scope instanceof Document);
		assertEquals("system", ((Document) scope).get("$ne"));
	}

	@Test
	void deactivate_removesRolePermissions() {
		setPermissions(List.of());
		Permission permission = new Permission();
		permission.setId("perm-1");
		permission.setOrganizationId("org-alpha");
		permission.setScope("tenant");
		permission.setActive(true);
		when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(permission));

		permissionService.deactivate("perm-1", "actor-1", "org-alpha", "127.0.0.1", "agent");

		ArgumentCaptor<Permission> permissionCaptor = ArgumentCaptor.forClass(Permission.class);
		verify(permissionRepository).save(permissionCaptor.capture());
		assertEquals(Boolean.FALSE, permissionCaptor.getValue().getActive());
		verify(rolePermissionRepository).deleteByPermissionId("perm-1");
	}

	private void setPermissions(List<String> permissions) {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("permissions", permissions)
				.claim("org_id", "org-alpha")
				.subject("user-1")
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
	}
}
