package com.clinic.mgmt.reference.repository;

import java.util.Optional;
import com.clinic.mgmt.reference.domain.Clinic;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClinicRepository extends MongoRepository<Clinic, String> {
	Optional<Clinic> findByOrganizationIdAndCode(String organizationId, String code);
}
