package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.PasswordResetToken;
import com.maxcogito.auth.mfa.OtpGenerator;
import com.maxcogito.auth.repo.PasswordResetTokenRepository;
import com.maxcogito.auth.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepo;
    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepo;
    private final GraphMailService mailer;
    private final PasswordEncoder encoder;
    private final OtpGenerator otpGenerator;
    private final String frontendBaseUrl;

    private static final Duration TTL = Duration.ofMinutes(15);

    public PasswordResetService(UserRepository userRepo,
                                PasswordResetTokenRepository tokenRepo,
                                UserService userService,
                                GraphMailService mailer,
                                PasswordEncoder encoder,
                                OtpGenerator otpGenerator,
                                @Value("${app.frontendBaseUrl:http://localhost:5173}") String frontendBaseUrl) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.tokenRepo = tokenRepo;
        this.mailer = mailer;
        this.encoder = encoder;
        this.otpGenerator = otpGenerator;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public void start(String email) {
     //   var user = userRepo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No user with that email"));

        var user = userService.findByUsernameOrEmail(email);
        // Invalidate older tokens
        tokenRepo.deleteByUserId(user.get().getId());

        Logger log = LoggerFactory.getLogger(PasswordResetService.class);

        log.info("Starting password reset for user {}", user);

        //String token = UUID.randomUUID().toString().replace("-", "");
        var prt = new PasswordResetToken();
        //String hash = sha256(token);
        String code = otpGenerator.generate6();
        String hash = sha256(code);
        prt.setToken(hash);
        //prt.setToken(code);
        prt.setUser(user.get());
        prt.setExpiresAt(Instant.now().plus(TTL));
        tokenRepo.save(prt);

        String subject = "Reset your password";
        String html = """
            <p>Hello %s,</p>
            <p>Use this code to reset your password:</p>
            <h2 style="letter-spacing:2px;">%s</h2>
            <p>This code expires in %d minutes.</p>
            <p>If you didn’t request this, you can ignore this email.</p>
        """.formatted(user.get().getFirstName() == null ? user.get().getUsername() : user.get().getFirstName(),
                code, TTL.toMinutes());

        mailer.sendHtml(user.get().getEmail(), subject, html);
    }

    @Transactional
    public void confirm(String token, String newPassword) {

        String normalized = token.trim();

        String hash = sha256(normalized);
        var prt = tokenRepo.findByToken(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (prt.isConsumed()) throw new IllegalArgumentException("Token already used");
        if (prt.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalArgumentException("Password reset token expired");

        var user = prt.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(user);

        prt.setConsumed(true);
        tokenRepo.save(prt);
    }

    private static String sha256(String s) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return java.util.HexFormat.of().formatHex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

}
