package com.clinic.mgmt.identity.repository;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.identity.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByPhone(String phone);

	Optional<User> findByOrganizationIdAndEmail(String organizationId, String email);

	Optional<User> findByOrganizationIdAndPhone(String organizationId, String phone);

	List<User> findByOrganizationId(String organizationId);

	List<User> findByOrganizationIdAndStatus(String organizationId, String status);

	List<User> findByOrganizationIdAndRoleCodes(String organizationId, String roleCode);

	List<User> findByOrganizationIdAndStatusAndRoleCodes(
			String organizationId,
			String status,
			String roleCode
	);

	boolean existsByRoleIdsContains(String roleId);
}
