package com.maxcogito.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
@EnableConfigurationProperties(AuthProperties.class)
public record AuthProperties(boolean defaultMfaRequired,
                             int onboardingTokenMinutes) {}
