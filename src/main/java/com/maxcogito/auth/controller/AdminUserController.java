package com.maxcogito.auth.controller;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.RoleUpdateRequest;
import com.maxcogito.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;
    public AdminUserController(UserService userService) { this.userService = userService; }

    // Replace the entire role set for a user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{username}/roles")
    public ResponseEntity<Void> setRoles(@PathVariable String username,
                                         @Valid @RequestBody RoleUpdateRequest req) {
        userService.setRolesForUser(username, req.roles());
        return ResponseEntity.noContent().build();
    }

    // Add one role

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{username}/roles/add")
    public ResponseEntity<Set<String>> addRole(@PathVariable String username,
                                               @RequestParam("role") String role) {
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
}
