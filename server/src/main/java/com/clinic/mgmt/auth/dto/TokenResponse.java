package com.clinic.mgmt.auth.dto;

public class TokenResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresIn;
	private AuthUserResponse user;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public AuthUserResponse getUser() {
		return user;
	}

	public void setUser(AuthUserResponse user) {
		this.user = user;
	}
}
