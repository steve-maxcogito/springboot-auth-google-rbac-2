package com.maxcogito.auth.security;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Component
public class JwtService {

    private final Key signingKey;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secretBase64,        // already Base64 !!!
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expirationMinutes}") long expirationMinutes
    ) {
        // ✅ FIX: decode the provided Base64 secret directly
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(String subject, String username, String email,
                              Set<String> roles) {
        return createToken(subject, username, email, roles, Map.of());
    }

    public String createToken(String subject, String username, String email,
                              Set<String> roles,
                              Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        JwtBuilder builder = Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject) // user id
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp));

        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach(builder::claim);
        }

        return builder.signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }

    /** Short-lived, limited-scope token for onboarding (email verify + MFA setup). */
    public String createOnboardingToken(String subject, String username, String email,
                                        Set<String> roles, int ttlMinutes, boolean mfaRequired) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60L);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .claim("scope", mfaRequired ? "onboarding mfa_required" : "onboarding")
                .claim("mfa_verified", false)
                .claim("email_verified", false)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
    }
}
