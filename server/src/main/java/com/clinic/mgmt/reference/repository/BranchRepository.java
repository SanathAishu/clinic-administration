package com.clinic.mgmt.reference.repository;

import java.util.Optional;
import com.clinic.mgmt.reference.domain.Branch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BranchRepository extends MongoRepository<Branch, String> {
	Optional<Branch> findByOrganizationIdAndCode(String organizationId, String code);
	Optional<Branch> findByClinicIdAndCode(String clinicId, String code);
}
