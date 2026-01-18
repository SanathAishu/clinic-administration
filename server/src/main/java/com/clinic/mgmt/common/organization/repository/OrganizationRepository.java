package com.clinic.mgmt.common.organization.repository;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.common.organization.domain.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrganizationRepository extends MongoRepository<Organization, String> {
	Optional<Organization> findByCode(String code);

	List<Organization> findByActive(boolean active);
}
