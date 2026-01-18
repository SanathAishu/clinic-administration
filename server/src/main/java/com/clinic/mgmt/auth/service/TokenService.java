package com.clinic.mgmt.auth.service;

import java.time.Instant;
import java.util.List;
import com.clinic.mgmt.identity.domain.User;
import com.clinic.mgmt.security.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public TokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public String createAccessToken(User user, List<String> permissions) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(jwtProperties.getAccessTokenTtl());
		List<String> safePermissions = permissions == null ? List.of() : permissions;
		List<String> roleCodes = user.getRoleCodes() == null ? List.of() : user.getRoleCodes();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.getIssuer())
				.subject(user.getId())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("org_id", user.getOrganizationId())
				.claim("roles", roleCodes)
				.claim("permissions", safePermissions)
				.claim("name", user.getFullName())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long accessTokenTtlSeconds() {
		return jwtProperties.getAccessTokenTtl().toSeconds();
	}
}
