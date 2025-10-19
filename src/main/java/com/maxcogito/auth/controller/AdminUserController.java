package com.maxcogito.auth.controller;
import com.maxcogito.auth.domain.Role;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.RoleUpdateRequest;
import com.maxcogito.auth.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    private final UserService userService;
    public AdminUserController(UserService userService) { this.userService = userService; }

    // Replace the entire role set for a user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{username}/replace/roles")
    public ResponseEntity<Void> setNewRoles(@PathVariable String username,
                                         @Valid @RequestBody RoleUpdateRequest req) {
        userService.setRolesForUser(username, req.roles());
        return ResponseEntity.noContent().build();
    }

    // Replace entire role set (idempotent)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{username}/roles")
    public ResponseEntity<Set<String>> setRoles(@PathVariable String username,
                                                @Valid @RequestBody RoleUpdateRequest req) {
        Set<String> normalized = req.roles().stream()
                .map(this::normalizeRole).collect(java.util.stream.Collectors.toSet());
        Set<Role> result = userService.setRolesListForUser(username, normalized);
        Set<String> roleStrings = result.stream().map(Role::getName).collect(Collectors.toSet());
        return ResponseEntity.ok(roleStrings);
    }

    // Add one role

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{username}/roles/add")
    public ResponseEntity<Set<String>> addRole(@PathVariable String username,
                                               @RequestParam("role") String role) {
        log.info("Received add role request: {}", role);
        log.info("Received add role request for user: {}", username);
        return ResponseEntity.ok(userService.addRoleToUser(username, role));
    }

    // Remove one role
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{username}/roles/remove")
    public ResponseEntity<Void> removeRole(@PathVariable String username,
                                           @RequestParam("role") String role) {
        userService.removeRoleFromUser(username, role);
        return ResponseEntity.noContent().build();
    }

    // Remove one role
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{username}/roles/{role}")
    public ResponseEntity<Set<String>> removeUserRole(@PathVariable String username,
                                                  @PathVariable String role) {
        Set<Role> updated = userService.removeRoleSetFromUser(username, normalizeRole(role));
        return ResponseEntity.ok(updated.stream().map(Role::getName).collect(Collectors.toSet()));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("role required");
        role = role.trim().toUpperCase();
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

}
