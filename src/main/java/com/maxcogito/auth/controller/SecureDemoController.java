package com.maxcogito.auth.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecureDemoController {

    @GetMapping("/api/v1/user/ping")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SECURITY_SERVICE','DATA_SERVICE')")
    public String userPing() { return "Hello, authenticated user"; }

    @GetMapping("/api/v1/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPing() { return "Hello, admin"; }

    @GetMapping("/api/v1/security/ping")
    @PreAuthorize("hasRole('SECURITY_SERVICE')")
    public String securityPing() { return "Hello, security service user"; }

    @GetMapping("/api/v1/data/ping")
    @PreAuthorize("hasRole('DATA_SERVICE')")
    public String dataPing() { return "Hello, data service user"; }
}
