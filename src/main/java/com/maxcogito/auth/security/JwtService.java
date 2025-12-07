package com.maxcogito.auth.security;

import com.maxcogito.auth.domain.Role;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.TokenPairResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtService {
    private final Key signingKey;
    private final String issuer;
    private final long expirationMinutes;

    // Add these (read from application.properties you already have)
    private final long refreshTtlDays;
    private final boolean refreshRotateOnUse;
    private final int maxActiveTokens;

    public JwtService(
            @Value("${app.jwt.secret}") String secretBase64,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expirationMinutes}") long expirationMinutes,
            @Value("${app.refresh.tokenTtlDays}") long refreshTtlDays,
            @Value("${app.refresh.rotateOnUse:true}") boolean refreshRotateOnUse,
            @Value("${app.jwt.max-active-tokens}") int maxActiveTokens
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
        this.refreshTtlDays = refreshTtlDays;
        this.refreshRotateOnUse = refreshRotateOnUse;
        this.maxActiveTokens = maxActiveTokens;
    }

    // ---------- existing createToken overloads stay unchanged ----------
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
                .setSubject(subject)
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

    // ---------- new helpers that build on your existing methods ----------

    public String createAccessToken(User user, Map<String, Object> extraClaims) {
        return createToken(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                roleNames(user),
                extraClaims
        );
    }

    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(refreshTtlDays));

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("token_type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** What your controller will call after successful MFA verification. */
    public TokenPairResponse issueTokenPair(User user, Map<String, Object> extraClaims) {
        String access = createAccessToken(user, extraClaims);
        String refresh = createRefreshToken(user);
        return new TokenPairResponse(
                access,
                user.getUsername(),
                user.getEmail(),
                roleNames(user),
                refresh
        );
    }

    /** Exchange a refresh token for a new access token (and optionally rotate refresh). */
    public TokenPairResponse refresh(String refreshToken, User user, Map<String, Object> extraClaims) {
        Claims claims = parseClaims(refreshToken);

        // Must be refresh token
        if (!"refresh".equals(claims.get("token_type"))) {
            throw new IllegalArgumentException("Invalid token type");
        }
        // Subject must match this user
        if (!user.getId().toString().equals(claims.getSubject())) {
            throw new IllegalArgumentException("Token subject mismatch");
        }

        String newAccess = createAccessToken(user, extraClaims);
        String outRefresh = refreshToken;

        if (refreshRotateOnUse) {
            outRefresh = createRefreshToken(user);
        }

        return new TokenPairResponse(
                newAccess,
                user.getUsername(),
                user.getEmail(),
                roleNames(user),
                outRefresh
        );
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

    public String createMfaAccessToken(User user, Set<String> roles, boolean mfa) {
        var extra = java.util.Map.<String, Object>of("mfa", mfa);
        return createToken(user.getId().toString(), user.getUsername(), user.getEmail(), roles, extra);
    }


    public io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }
}
