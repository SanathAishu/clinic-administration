package com.clinic.mgmt.treatment.repository;

import java.util.Optional;
import com.clinic.mgmt.treatment.domain.TreatmentType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TreatmentTypeRepository extends MongoRepository<TreatmentType, String> {
	Optional<TreatmentType> findByClinicIdAndName(String clinicId, String name);
}
