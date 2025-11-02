package com.maxcogito.auth.controller;
import com.maxcogito.auth.domain.Role;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.AdminUpdateUserRequest;
import com.maxcogito.auth.dto.RegisterRequest;
import com.maxcogito.auth.dto.RoleUpdateRequest;
import com.maxcogito.auth.dto.UserProfileResponse;
import com.maxcogito.auth.service.UserService;
import com.maxcogito.auth.service.VerificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    private final UserService userService;
    private final VerificationService verificationService;
    public AdminUserController(UserService userService, VerificationService verificationService)
    { this.userService = userService;
    this.verificationService = verificationService;
    }

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
    @PutMapping("/{usernameOrEmail:.+}/roles")
    public ResponseEntity<Set<String>> setRoles(@PathVariable String username,
                                                @Valid @RequestBody RoleUpdateRequest req) {
        Set<String> normalized = req.roles().stream()
                .map(this::normalizeRole).collect(java.util.stream.Collectors.toSet());
        Set<Role> result = userService.setRolesListForUser(username, normalized);
        Set<String> roleStrings = result.stream().map(Role::getName).collect(Collectors.toSet());
        return ResponseEntity.ok(roleStrings);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{usernameOrEmail:.+}/verification/send-email")
    public ResponseEntity<Void> sendVerificationEmail(
            @PathVariable("usernameOrEmail") String usernameOrEmail) {

        // If your service returns Optional<User>, handle 404; otherwise just load:
        var opt = userService.findByUsernameOrEmail(usernameOrEmail);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        verificationService.startVerification(opt.get());
        return ResponseEntity.noContent().build(); // 204 matches the frontend's void
    }


    // Replace entire role set (idempotent)

    // .+ to allow dots in emails/usernames
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usernameOrEmail:.+}")
    public ResponseEntity<Void> deleteUser(@PathVariable String usernameOrEmail) {
        userService.deleteByUsernameOrEmail(usernameOrEmail);
        return ResponseEntity.noContent().build(); // 204 on success
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path= "/user/{username}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getUser(@PathVariable String username) {
        log.debug("REST request to get single User");
        Optional<User> user = userService.findByUsernameOrEmail(username);
        return ResponseEntity.ok(user.get());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable String username,
            @Valid @RequestBody AdminUpdateUserRequest req) {

        log.info("Admin updating user profile for {}", username);

        return userService.findByUsernameOrEmail(username)
                .map(user -> {
                    boolean emailChanged = false;

                    if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(user.getEmail())) {
                        user.setEmail(req.getEmail().trim());
                        // If your model has emailVerified flag:
                        try { user.getClass().getMethod("setEmailVerified", boolean.class);
                            emailChanged = true;
                        } catch (NoSuchMethodException ignore) {}
                    }

                    if (req.getFirstName()   != null) user.setFirstName(req.getFirstName());
                    if (req.getMiddleName()  != null) user.setMiddleName(req.getMiddleName());
                    if (req.getLastName()    != null) user.setLastName(req.getLastName());
                    if (req.getMfaRequired() != null) user.setMfaRequired(req.getMfaRequired());

                    if (req.getAddressLine1()!= null) user.setAddressLine1(req.getAddressLine1());
                    if (req.getAddressLine2()!= null) user.setAddressLine2(req.getAddressLine2());
                    if (req.getCity()        != null) user.setCity(req.getCity());
                    if (req.getState()       != null) user.setState(req.getState());
                    if (req.getPostalCode()  != null) user.setPostalCode(req.getPostalCode());
                    if (req.getCountry()     != null) user.setCountry(req.getCountry());
                    if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());

                    // If your entity has emailVerified boolean, auto-invalidate on change:
                    try {
                        if (emailChanged) {
                            var m = user.getClass().getMethod("setEmailVerified", boolean.class);
                            m.invoke(user, false);
                        }
                    } catch (Exception ignore) {}

                    User saved = userService.save(user);

                    UserProfileResponse out = new UserProfileResponse();
                    out.setId(saved.getId() != null ? saved.getId().toString() : null);
                    out.setUsername(saved.getUsername());
                    out.setEmail(saved.getEmail());
                    try {
                        var gm = saved.getClass().getMethod("isEmailVerified");
                        out.setEmailVerified((Boolean) gm.invoke(saved));
                    } catch (Exception ignore) {}
                    out.setFirstName(saved.getFirstName());
                    out.setMiddleName(saved.getMiddleName());
                    out.setLastName(saved.getLastName());
                    out.setMfaRequired(saved.isMfaRequired());

                    out.setAddressLine1(saved.getAddressLine1());
                    out.setAddressLine2(saved.getAddressLine2());
                    out.setCity(saved.getCity());
                    out.setState(saved.getState());
                    out.setPostalCode(saved.getPostalCode());
                    out.setCountry(saved.getCountry());
                    out.setPhoneNumber(saved.getPhoneNumber());

                    // roles -> Set<String>
                    out.setRoles(saved.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));

                    return ResponseEntity.ok(out);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private static final Set<String> ALLOWED = Set.of(
            "ROLE_USER", "ROLE_ADMIN", "ROLE_DATA_SERVICE", "ROLE_SECURITY_SERVICE"
    );

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("role required");
        String r = role.trim().toUpperCase();
        r = r.startsWith("ROLE_") ? r : "ROLE_" + r;
        if (!ALLOWED.contains(r)) throw new IllegalArgumentException("unknown role: " + r);
        return r;
    }


}
