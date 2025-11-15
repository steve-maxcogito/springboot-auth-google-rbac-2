package com.maxcogito.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mfa")
public record MfaProperties(boolean required,          // app.mfa.required=true/false
                            int loginTtlMinutes, //app.mfa.login.ttlMinutes,
                            int resendCooldownSeconds, // app.mfa.resend.cooldownSeconds
                            int onboardingTtlMinutes,
                            int maxAttempts,
                            int stepUpMaxAgeSeconds,
                            String method)  // app.mfa.method=[email,sms]
 {}
