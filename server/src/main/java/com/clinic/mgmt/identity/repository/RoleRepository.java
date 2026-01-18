package com.clinic.mgmt.identity.repository;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.identity.domain.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {

	List<Role> findByOrganizationId(String organizationId);

	Optional<Role> findByOrganizationIdAndRoleCode(String organizationId, String roleCode);

	Optional<Role> findByOrganizationIdAndName(String organizationId, String name);

	List<Role> findByIdIn(List<String> ids);

	List<Role> findByOrganizationIdAndRoleCodeIn(String organizationId, List<String> roleCodes);
}
