package com.clinic.mgmt.visit.repository;

import com.clinic.mgmt.visit.domain.HouseVisit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HouseVisitRepository extends MongoRepository<HouseVisit, String> {
}
