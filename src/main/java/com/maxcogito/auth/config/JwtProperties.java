package com.maxcogito.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer,Integer expirationMinutes,Integer maxActiveTokens)
{}
