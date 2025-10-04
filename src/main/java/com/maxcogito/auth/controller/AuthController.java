package com.maxcogito.auth.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.*;
import com.maxcogito.auth.google.GoogleTokenVerifier;
import com.maxcogito.auth.security.JwtService;
import com.maxcogito.auth.security.UserDetailsImpl;
import com.maxcogito.auth.security.UserDetailsServiceImpl;
import com.maxcogito.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.maxcogito.auth.dto.TokenPairResponse;
import com.maxcogito.auth.dto.RefreshRequest;
import com.maxcogito.auth.dto.EmailRequest;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserDetailsServiceImpl userDetailsService;
    private final com.maxcogito.auth.service.RefreshTokenService refreshTokenService;
    private final com.maxcogito.auth.service.VerificationService verificationService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtService jwtService,
                          GoogleTokenVerifier googleTokenVerifier,
                          UserDetailsServiceImpl userDetailsService,
                          com.maxcogito.auth.service.RefreshTokenService refreshTokenService,
                          com.maxcogito.auth.service.VerificationService verificationService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setFirstName(req.getFirstName());
        u.setMiddleName(req.getMiddleName());
        u.setLastName(req.getLastName());
        u.setAddressLine1(req.getAddressLine1());
        u.setAddressLine2(req.getAddressLine2());
        u.setCity(req.getCity());
        u.setState(req.getState());
        u.setPostalCode(req.getPostalCode());
        u.setCountry(req.getCountry());
        u.setPhoneNumber(req.getPhoneNumber());

        User saved = userService.registerLocal(u, req.getRoles(), req.getPassword());

        var roles = saved.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());
        String token = jwtService.createToken(saved.getId().toString(), saved.getUsername(), saved.getEmail(), roles);
        String rt = refreshTokenService.createToken(saved);
        return ResponseEntity.ok(new TokenPairResponse(token, saved.getUsername(), saved.getEmail(), roles, rt));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        var principal = (UserDetailsImpl) auth.getPrincipal();
        var user = principal.getDomainUser();
        var roles = principal.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
        String token = jwtService.createToken(user.getId().toString(), user.getUsername(), user.getEmail(), roles);
        String rt = refreshTokenService.createToken(user);
        return ResponseEntity.ok(new TokenPairResponse(token, user.getUsername(), user.getEmail(), roles, rt));
    }

    @PostMapping("/google")
    public ResponseEntity<?> google(@Valid @RequestBody GoogleLoginRequest req) throws Exception {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(req.getIdToken());
        if (payload == null) {
            return ResponseEntity.status(401).body("Invalid Google ID token");
        }
        String email = payload.getEmail();
        String sub = payload.getSubject();
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");

        User saved = userService.upsertGoogleUser(email, sub, givenName, familyName);
        var roles = saved.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());
        String token = jwtService.createToken(saved.getId().toString(), saved.getUsername(), saved.getEmail(), roles);
        String rt = refreshTokenService.createToken(saved);
        return ResponseEntity.ok(new TokenPairResponse(token, saved.getUsername(), saved.getEmail(), roles, rt));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        var old = refreshTokenService.validate(req.getRefreshToken());
        var user = old.getUser();
        var roles = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet());
        String newAccess = jwtService.createToken(user.getId().toString(), user.getUsername(), user.getEmail(), roles);
        String newRefresh = refreshTokenService.createToken(user);
        return ResponseEntity.ok(new TokenPairResponse(newAccess, user.getUsername(), user.getEmail(), roles, newRefresh));
    }

    @PostMapping("/verify/start")
    public ResponseEntity<?> startVerify(@Valid @RequestBody EmailRequest req) {
        var user = userService.findByUsernameOrEmail(req.getEmail()).orElseThrow(() -> new IllegalArgumentException("No user with that email"));
        verificationService.startVerification(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<?> confirmVerify(@RequestParam("token") String token) {
        verificationService.confirm(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshRequest req) {
        refreshTokenService.revoke(req.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
        var principal = (UserDetailsImpl) userDetailsService.loadUserByUsername(auth.getName());
        var user = principal.getDomainUser();
        var roles = principal.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
        return ResponseEntity.ok(new JwtResponse("",
                user.getUsername(), user.getEmail(), roles));
    }
}
