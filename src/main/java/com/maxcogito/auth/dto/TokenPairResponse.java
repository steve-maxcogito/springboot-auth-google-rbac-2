package com.maxcogito.auth.dto;

public class TokenPairResponse extends JwtResponse {
    private String refreshToken;

    public TokenPairResponse(String accessToken, String username, String email, java.util.Set<String> roles, String refreshToken) {
        super(accessToken, username, email, roles);
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
