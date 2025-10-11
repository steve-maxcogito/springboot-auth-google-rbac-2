package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.domain.VerificationToken;
import com.maxcogito.auth.repo.UserRepository;
import com.maxcogito.auth.repo.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerificationTokenRepository repo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final GraphMailService mailer;
    private final long ttlMinutes;
    private final String frontendBaseUrl;
    private final SecureRandom sr = new SecureRandom();

    public VerificationService(VerificationTokenRepository repo, UserRepository userRepo,
                               EmailService emailService,
                               GraphMailService mailer,
                               @Value("${app.verification.tokenTtlMinutes:60}") long ttlMinutes,
                               @Value("${app.frontendBaseUrl:http://localhost:5173}") String frontendBaseUrl) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.mailer = mailer;
        this.ttlMinutes = ttlMinutes;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    private static final Duration TTL = Duration.ofMinutes(15);

    @Transactional
    public void startVerificationCode(User user) {
        // Invalidate any prior tokens
        repo.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(TTL);

        VerificationToken vt = new VerificationToken();
        vt.setToken(token);
        vt.setUser(user);
        vt.setExpiresAt(expiresAt);
        vt.setUsed(false);
        repo.save(vt);

        // Simple HTML email
        String subject = "Verify your email";
        String html = """
            <p>Hello %s,</p>
            <p>Use this code to verify your email address:</p>
            <h2 style="letter-spacing:2px;">%s</h2>
            <p>This code expires in %d minutes.</p>
            <p>If you didn’t request this, you can ignore this email.</p>
        """.formatted(user.getFirstName() == null ? user.getUsername() : user.getFirstName(),
                token, TTL.toMinutes());

        // send
        mailer.sendHtml(user.getEmail(), subject, html);
    }

    @Transactional
    public void confirmCode(String token) {
        var vt = repo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (vt.isUsed() || vt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired or already used");
        }
        vt.setUsed(true);
        repo.save(vt);
        // (Optional) You could track a 'verified' flag on User; add field if desired.
        // For simplicity, we don't block login on unverified status in this sample.
    }

    @Transactional
    public void startVerification(User user) {
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setToken(generate());
        vt.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
        repo.save(vt);

        String link = frontendBaseUrl + "/verify-email?token=" + vt.getToken();
        String body = "Hello " + (user.getFirstName()!=null?user.getFirstName():"") + ",\n\n"
                + "Please verify your email by clicking the link: " + link + "\n\n"
                + "This link expires in " + ttlMinutes + " minutes.";
        emailService.send(user.getEmail(), "Verify your email", body);
    }

    @Transactional
    public void confirm(String token) {
        var vt = repo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (vt.isUsed() || vt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired or already used");
        }
        vt.setUsed(true);
        repo.save(vt);
        // (Optional) You could track a 'verified' flag on User; add field if desired.
        // For simplicity, we don't block login on unverified status in this sample.
    }

    private String generate() {
        byte[] b = new byte[32];
        sr.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
