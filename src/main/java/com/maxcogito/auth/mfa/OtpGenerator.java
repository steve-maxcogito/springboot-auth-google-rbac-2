package com.maxcogito.auth.mfa;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {
    private final SecureRandom rnd = new SecureRandom();
    // 6-digit numeric code, no leading zeros issue
    public String generate6() {
        int n = rnd.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
