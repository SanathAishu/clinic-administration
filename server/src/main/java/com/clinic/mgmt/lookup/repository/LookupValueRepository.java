package com.clinic.mgmt.lookup.repository;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.lookup.domain.LookupValue;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LookupValueRepository extends MongoRepository<LookupValue, String> {
	Optional<LookupValue> findByLookupTypeAndCode(String lookupType, String code);

	List<LookupValue> findByLookupType(String lookupType);

	List<LookupValue> findByLookupTypeAndActive(String lookupType, boolean active);

	List<LookupValue> findByActive(boolean active);
}
