package com.clinic.mgmt.identity.repository;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.identity.domain.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PermissionRepository extends MongoRepository<Permission, String> {

	List<Permission> findByOrganizationId(String organizationId);

	Optional<Permission> findByOrganizationIdAndPermissionCode(String organizationId, String permissionCode);

	List<Permission> findByIdInAndActiveTrue(List<String> ids);
}
