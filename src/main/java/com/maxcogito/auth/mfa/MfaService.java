package com.maxcogito.auth.mfa;

import com.maxcogito.auth.domain.MfaChallenge;
import com.maxcogito.auth.domain.MfaMethod;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.repo.MfaChallengeRepository;
import com.maxcogito.auth.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;



@Service
public class MfaService {
    private final MfaChallengeRepository repo;
    private final OtpGenerator otp;
    private final PasswordEncoder encoder;
    private final EmailService mail;
    private final SmsService sms;
    private final Clock clock;

    @Value("${app.mfa.login.ttlMinutes:10}")
    private int ttlMinutes;

    @Value("${app.mfa.login.maxAttempts:5}")
    private int maxAttempts;

    @Value("${app.mfa.resend.cooldownSeconds:60}")
    private int resendCooldownSeconds;

    public MfaService(MfaChallengeRepository repo, OtpGenerator otp, PasswordEncoder encoder,
                      EmailService mail, SmsService sms, Clock clock) {
        this.repo = repo; this.otp = otp; this.encoder = encoder;
        this.mail = mail; this.sms = sms; this.clock = clock;
    }

    public MfaChallenge startLoginChallenge(User user) {
        var latest = repo.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, "LOGIN_MFA").orElse(null);
        if (latest != null && latest.getCreatedAt()
                .isAfter(Instant.now(clock).minus(resendCooldownSeconds, ChronoUnit.SECONDS))) {
            return latest; // throttle: reuse last in cooldown window
        }

        String code = otp.generate6();
        var ch = new MfaChallenge();
        ch.setUser(user);
        ch.setCodeHash(encoder.encode(code));
        ch.setExpiresAt(Instant.now(clock).plus(ttlMinutes, ChronoUnit.MINUTES));
        ch.setAttempts(0);
        ch.setPurpose("LOGIN_MFA");
        ch = repo.save(ch);

        // deliver
        if (user.getMfaMethod() == MfaMethod.EMAIL_OTP) {
            mail.send(user.getEmail(), "Your login code",
                    "Your verification code is: " + code + " (valid " + ttlMinutes + " minutes)");
        } else if (user.getMfaMethod() == MfaMethod.SMS_OTP) {
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                throw new IllegalArgumentException("User has no phone number on file");
            }
            sms.send(user.getPhoneNumber(), "Your code: " + code + " (valid " + ttlMinutes + "m)");
        } else {
            throw new IllegalStateException("Unsupported MFA method: " + user.getMfaMethod());
        }
        return ch;
    }

    public User verifyLoginCode(User user, UUID challengeId, String code) {
        var ch = repo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid challenge"));
        if (!ch.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Challenge not for this user");
        }
        var now = Instant.now(clock);
        if (now.isAfter(ch.getExpiresAt())) throw new IllegalArgumentException("Code expired");
        if (ch.getAttempts() >= maxAttempts) throw new IllegalArgumentException("Too many attempts");

        ch.setAttempts(ch.getAttempts() + 1);
        repo.save(ch);

        if (!encoder.matches(code, ch.getCodeHash())) {
            throw new IllegalArgumentException("Invalid code");
        }

        repo.delete(ch); // one-time use, success
        return user;
    }
}
