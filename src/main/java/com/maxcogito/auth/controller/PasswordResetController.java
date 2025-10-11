package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.PasswordResetConfirmRequest;
import com.maxcogito.auth.dto.PasswordResetStartRequest;
import com.maxcogito.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {
    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    // Step 1: start reset → sends email code
    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@Valid @RequestBody PasswordResetStartRequest req) {
        service.start(req.email());
        return ResponseEntity.accepted().build(); // 202 Accepted
    }

    // Step 2: confirm with code + set new password
    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        service.confirm(req.token(), req.newPassword());
        return ResponseEntity.noContent().build(); // 204
    }

}
