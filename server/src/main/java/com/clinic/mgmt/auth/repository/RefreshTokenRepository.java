package com.clinic.mgmt.auth.repository;

import java.util.Optional;
import com.clinic.mgmt.auth.domain.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
