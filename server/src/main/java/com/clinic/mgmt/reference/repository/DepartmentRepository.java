package com.clinic.mgmt.reference.repository;

import java.util.Optional;
import com.clinic.mgmt.reference.domain.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepartmentRepository extends MongoRepository<Department, String> {
	Optional<Department> findByClinicIdAndCode(String clinicId, String code);
}
