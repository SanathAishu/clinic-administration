package com.clinic.mgmt.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.jwt")
@Validated
public class JwtProperties {

	@NotBlank
	private String issuer;

	@NotBlank
	private String secret;

	@NotNull
	private Duration accessTokenTtl;

	@NotNull
	private Duration refreshTokenTtl;

	@NotNull
	@Positive
	private Integer refreshTokenBytes;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	public void setAccessTokenTtl(Duration accessTokenTtl) {
		this.accessTokenTtl = accessTokenTtl;
	}

	public Duration getRefreshTokenTtl() {
		return refreshTokenTtl;
	}

	public void setRefreshTokenTtl(Duration refreshTokenTtl) {
		this.refreshTokenTtl = refreshTokenTtl;
	}

	public Integer getRefreshTokenBytes() {
		return refreshTokenBytes;
	}

	public void setRefreshTokenBytes(Integer refreshTokenBytes) {
		this.refreshTokenBytes = refreshTokenBytes;
	}

	public SecretKey secretKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT secret must be at least 32 bytes");
		}
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}
}
