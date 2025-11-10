package com.maxcogito.auth.mfa;

import com.maxcogito.auth.config.MfaProperties;
import com.maxcogito.auth.domain.MfaChallenge;
import com.maxcogito.auth.domain.MfaMethod;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.repo.MfaChallengeRepository;
import com.maxcogito.auth.service.EmailService;
import com.maxcogito.auth.service.UserService;
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
    private final UserService userService;
    private final Clock clock;
    private final MfaProperties props;

    // You can make this configurable if you want attempts in properties too.

    public MfaService(MfaChallengeRepository repo,
                      OtpGenerator otp,
                      PasswordEncoder encoder,
                      EmailService mail,
                      SmsService sms,
                      UserService userService,
                      Clock clock,
                      MfaProperties props) {
        this.repo = repo;
        this.otp = otp;
        this.encoder = encoder;
        this.mail = mail;
        this.sms = sms;
        this.userService = userService;
        this.clock = clock;
        this.props = props;
    }

    public MfaChallenge startLoginChallenge(User user) {
        var latest = repo.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, "LOGIN_MFA").orElse(null);
        if (latest != null && latest.getCreatedAt()
                .isAfter(Instant.now(clock).minus(props.resendCooldownSeconds(), ChronoUnit.SECONDS))) {
            return latest; // throttle: reuse last within cooldown
        }

        String code = otp.generate6();
        var ch = new MfaChallenge();
        ch.setUser(user);
        ch.setCodeHash(encoder.encode(code));
        ch.setExpiresAt(Instant.now(clock).plus(props.loginTtlMinutes(), ChronoUnit.MINUTES));
        ch.setAttempts(0);
        ch.setPurpose("LOGIN_MFA");
        ch = repo.save(ch);

        deliverCode(user, code, props.loginTtlMinutes());
        return ch;
    }

    private void deliverCode(User user, String code, int ttlMinutes) {
        MfaMethod method = user.getMfaMethod();
        if (method == null) {
            // Fallback to global default if user preference is unset
            method = "sms".equalsIgnoreCase(props.method()) ? MfaMethod.SMS_OTP : MfaMethod.EMAIL_OTP;
        }

        switch (method) {
            case EMAIL_OTP -> {
                mail.send(
                        user.getEmail(),
                        "Your login code",
                        "Your verification code is: " + code + " (valid " + ttlMinutes + " minutes)"
                );
                user.setMfaMethod(method);
                userService.save(user);
            }
            case SMS_OTP -> {
                var phone = user.getPhoneNumber();
                if (phone == null || phone.isBlank()) {
                    throw new IllegalArgumentException("User has no phone number on file");
                }
                sms.send(phone, "Your code: " + code + " (valid " + ttlMinutes + "m)");
                user.setMfaMethod(method);
                userService.save(user);
            }
            default -> throw new IllegalStateException("Unsupported MFA method: " + method);
        }
    }

    public User verifyLoginCode(User user, UUID challengeId, String code) {
        var ch = repo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid challenge"));

        if (!ch.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Challenge not for this user");
        }
        var now = Instant.now(clock);
        if (now.isAfter(ch.getExpiresAt())) throw new IllegalArgumentException("Code expired");
        if (ch.getAttempts() >= props.maxAttempts()) throw new IllegalArgumentException("Too many attempts");

        ch.setAttempts(ch.getAttempts() + 1);
        repo.save(ch);

        if (!encoder.matches(code, ch.getCodeHash())) {
            throw new IllegalArgumentException("Invalid code");
        }

        repo.delete(ch); // one-time use
        return user;
    }

    // Add these to your existing MfaService

    public User verifyLoginCode(UUID challengeId, String code) {
        var ch = repo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid challenge"));

        var now = Instant.now(clock);
        if (now.isAfter(ch.getExpiresAt())) throw new IllegalArgumentException("Code expired");
        if (ch.getAttempts() >= props.maxAttempts()) throw new IllegalArgumentException("Too many attempts");

        ch.setAttempts(ch.getAttempts() + 1);
        repo.save(ch);

        if (!encoder.matches(code, ch.getCodeHash())) {
            throw new IllegalArgumentException("Invalid code");
        }

        var user = ch.getUser();
        repo.delete(ch); // one-time use on success
        return user;
    }

    /** Helper for resend-by-challenge flows */
    public User userForChallenge(UUID challengeId) {
        var ch = repo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid challenge"));
        return ch.getUser();
    }

}
