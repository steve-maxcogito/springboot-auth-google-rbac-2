package com.maxcogito.auth.controller;

import com.maxcogito.auth.config.AuthProperties;
import com.maxcogito.auth.config.MfaProperties;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.RegisterRequest;
import com.maxcogito.auth.dto.RegisterResponse;
import com.maxcogito.auth.repo.UserRepository;
import com.maxcogito.auth.security.JwtService;
import com.maxcogito.auth.service.UserService;
import com.maxcogito.auth.service.VerificationService;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// RegisterController.java
@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {
    private final PasswordEncoder pw;
    private final JwtService jwtService;
    private final UserService userService;
    private final MfaProperties mfaProperties;
    VerificationService verificationService;


    public RegisterController(UserService userService, PasswordEncoder pw, JwtService jwtService, VerificationService verificationService,MfaProperties mfaProps) {
        this.userService = userService;this.pw = pw; this.jwtService = jwtService; this.verificationService= verificationService;this.mfaProperties = mfaProps;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
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

        boolean requireMfa = (req.getMfaRequired() != null)
                ? req.getMfaRequired()
                : mfaProperties.required();

        saved.setMfaRequired(requireMfa);
        userService.save(saved);

    //    verificationService.startVerificationCode(saved);
        verificationService.startVerification(saved);

        var roles = saved.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());

        String onboarding = jwtService.createOnboardingToken(
                saved.getId().toString(),
                saved.getUsername(),
                saved.getEmail(),
                roles,
                mfaProperties.onboardingTtlMinutes(),
                requireMfa
        );

        Map<String, Object> body = Map.of(
                "onboardingToken", onboarding,
                "expiresInSeconds", (long) mfaProperties.onboardingTtlMinutes() * 60,
                "emailVerificationRequired", true,
                "mfaRequired", requireMfa
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
