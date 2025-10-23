package com.maxcogito.auth.controller;


import com.maxcogito.auth.config.MfaProperties;
import com.maxcogito.auth.domain.Role;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.TokenPairResponse;
import com.maxcogito.auth.mfa.MfaService;
import com.maxcogito.auth.security.JwtService;
import com.maxcogito.auth.service.UserService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class MfaController {

    private static final Logger log = LoggerFactory.getLogger(MfaController.class);
    private final AuthenticationManager authManager;
    private final UserService userService;
    private final MfaService mfaService;
    private final JwtService jwtService;
    private final MfaProperties mfaProps;

    public MfaController(AuthenticationManager authManager,
                         UserService userService,
                         MfaService mfaService,
                         JwtService jwtService,
                         MfaProperties mfaProps) {
        this.authManager = authManager;
        this.userService = userService;
        this.mfaService = mfaService;
        this.jwtService = jwtService;
        this.mfaProps = mfaProps;
    }

    // ------------------ STEP 1: PRIMARY LOGIN (starts MFA if required) ------------------
    @PostMapping("/mfalogin")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        String username = auth.getName();
        //var springUser = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
        User user = userService.loadDomainUserByUsername(username);

        //boolean requiresMfa = mfaProps.required() || Boolean.TRUE.equals(user.isMfaRequired());
        boolean requiresMfa = mfaProps.required();
        if (!requiresMfa) {
            // MFA not required -> issue full tokens immediately
                var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
                var extra = java.util.Map.<String, Object>of("mfa", true);
                var access  = jwtService.createToken(user.getId().toString(), user.getUsername(), user.getEmail(), roles, extra);
                var refresh = jwtService.createRefreshToken(user);
                return ResponseEntity.ok(new TokenPairResponse(access, user.getUsername(), user.getEmail(), roles, refresh));
         //   var tokens = jwtService.issueTokenPair(user, Map.of("mfa", true));
           // return ResponseEntity.ok(tokens);
        }

        // MFA required -> start challenge (this sends the email/SMS code!)
        var challenge = mfaService.startLoginChallenge(user);
        log.info("REQUIRES MFA: Inside mfalogin - with challenge {}", challenge);

        // IMPORTANT: Do NOT include the OTP in the response. Just metadata.
        return ResponseEntity.accepted().body(new MfaStartResponse(
                challenge.getId(),
                maskDestination(user),
                mfaProps.loginTtlMinutes()
        ));
    }

    // ------------------ STEP 2: VERIFY MFA (no username) ------------------
    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
        var user = mfaService.verifyLoginCode(req.challengeId(), req.code());
        var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        var extra = java.util.Map.<String, Object>of("mfa", true);
        var access = jwtService.createToken(user.getId().toString(),user.getUsername(),user.getEmail(),roles,extra);
        var refresh = jwtService.createRefreshToken(user);
        //var tokens = jwtService.issueTokenPair(user, Map.of("mfa", true));
        return ResponseEntity.ok(new TokenPairResponse(access, user.getUsername(), user.getEmail(), roles, refresh));
        //return ResponseEntity.ok(tokens);
    }

    // ------------------ OPTIONAL: RESEND (throttled in service) ------------------
    @PostMapping("/mfa/resend")
    public ResponseEntity<?> resend(@RequestBody ResendRequest req) {
        // Find the user tied to the existing challengeId
        var user = mfaService.userForChallenge(req.challengeId());
        var ch = mfaService.startLoginChallenge(user); // service reuses if within cooldown
        return ResponseEntity.accepted().body(new MfaStartResponse(
                ch.getId(),
                maskDestination(user),
                mfaProps.loginTtlMinutes()
        ));
    }

    // ------------------ OPTIONAL: START MFA FOR ALREADY-AUTH'D SESSION (step-up) ------------------
    @PostMapping("/mfa/restart-session")
    public ResponseEntity<?> startForPrevCurrentSession() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        var principal = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
        User user = userService.loadDomainUserByUsernameOrEmail(principal.getUsername());

        var ch = mfaService.startLoginChallenge(user);
        return ResponseEntity.accepted().body(new MfaStartResponse(
                ch.getId(),
                maskDestination(user),
                mfaProps.loginTtlMinutes()
        ));
    }

    @PostMapping("/mfa/start-session")
    public ResponseEntity<?> startForCurrentSession() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        com.maxcogito.auth.domain.User user;
        Object principal = auth.getPrincipal();

        if (principal instanceof com.maxcogito.auth.security.UserDetailsImpl udi) {
            // Best: load a fresh, managed entity by ID
            user = userService.loadDomainUserById(udi.getId());
        } else {
            // Generic fallback (e.g., other auth types)
            String username = auth.getName();
            user = userService.loadDomainUserByUsernameOrEmail(username);
        }

        var ch = mfaService.startLoginChallenge(user);
        return ResponseEntity.accepted().body(new MfaStartResponse(
                ch.getId(),
                maskDestination(user),
                mfaProps.loginTtlMinutes()
        ));
    }

    // ------------------ helpers & DTOs ------------------

    private String maskDestination(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            var e = user.getEmail();
            int at = e.indexOf('@');
            if (at > 1) return e.charAt(0) + "***" + e.substring(at - 1);
            return "***";
        }
        var p = user.getPhoneNumber();
        if (p != null && p.length() >= 4) return "***" + p.substring(p.length() - 4);
        return "***";
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record VerifyRequest(@NotBlank UUID challengeId, @NotBlank String code) {}
    public record ResendRequest(@NotBlank UUID challengeId) {}
    public record MfaStartResponse(UUID challengeId, String destinationMasked, int ttlMinutes) {}
}
