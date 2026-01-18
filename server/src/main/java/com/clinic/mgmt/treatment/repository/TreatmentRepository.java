package com.clinic.mgmt.treatment.repository;

import com.clinic.mgmt.treatment.domain.Treatment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TreatmentRepository extends MongoRepository<Treatment, String> {
}
