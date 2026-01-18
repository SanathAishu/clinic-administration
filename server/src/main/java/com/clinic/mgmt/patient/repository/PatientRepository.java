package com.clinic.mgmt.patient.repository;

import java.util.Optional;
import com.clinic.mgmt.patient.domain.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<Patient, String> {
	Optional<Patient> findByClinicIdAndPhoneAndStatusNot(String clinicId, String phone, String status);
}
