package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.PasswordResetToken;
import com.maxcogito.auth.repo.PasswordResetTokenRepository;
import com.maxcogito.auth.repo.UserRepository;
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
    private final PasswordResetTokenRepository tokenRepo;
    private final GraphMailService mailer;
    private final PasswordEncoder encoder;
    private final String frontendBaseUrl;

    private static final Duration TTL = Duration.ofMinutes(15);

    public PasswordResetService(UserRepository userRepo,
                                PasswordResetTokenRepository tokenRepo,
                                GraphMailService mailer,
                                PasswordEncoder encoder,
                                @Value("${app.frontendBaseUrl:http://localhost:5173}") String frontendBaseUrl) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.mailer = mailer;
        this.encoder = encoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public void start(String email) {
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user with that email"));

        // Invalidate older tokens
        tokenRepo.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        var prt = new PasswordResetToken();
        String hash = sha256(token);
        prt.setToken(hash);
        prt.setUser(user);
        prt.setExpiresAt(Instant.now().plus(TTL));
        tokenRepo.save(prt);

        String subject = "Reset your password";
        String html = """
            <p>Hello %s,</p>
            <p>Use this code to reset your password:</p>
            <h2 style="letter-spacing:2px;">%s</h2>
            <p>This code expires in %d minutes.</p>
            <p>If you didn’t request this, you can ignore this email.</p>
        """.formatted(user.getFirstName() == null ? user.getUsername() : user.getFirstName(),
                hash, TTL.toMinutes());

        mailer.sendHtml(user.getEmail(), subject, html);
    }

    @Transactional
    public void confirm(String token, String newPassword) {
        var prt = tokenRepo.findByToken(token)
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
