package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.AuthProvider;
import com.maxcogito.auth.domain.Role;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.errors.RoleNotFoundException;
import com.maxcogito.auth.errors.UserNotFoundException;
import com.maxcogito.auth.repo.RoleRepository;
import com.maxcogito.auth.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsernameOrEmail(String s) {
        return userRepository.findByUsername(s).or(() -> userRepository.findByEmail(s));
    }

    @Transactional
    public void setRolesForUser(String username, Set<String> roleNames) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        Set<Role> roles = resolveRoles(roleNames);
        user.setRoles(roles);          // assuming User has setRoles(Set<Role>)
        userRepository.save(user);
    }

    // UserService.java
    @Transactional
    public Set<String> addRoleToUser(String username, String roleName) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // normalize to ROLE_* and validate
        String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

        var role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new RoleNotFoundException(normalized));

        user.getRoles().add(role);          // Set prevents duplicates in memory
        userRepository.save(user);                // relies on unique constraint at DB (recommended)

        return user.getRoles()
                .stream().map(r -> r.getName())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Transactional
    public List<User> findAll(){
        List<User> users = userRepository.findAll();
        return users;
    };


    @Transactional
    public void removeRoleFromUser(String username, String roleName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Transactional
    public User registerLocal(User u, Set<String> roleNames, String rawPassword) {
        if (userRepository.existsByUsername(u.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(u.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        u.setProvider(AuthProvider.LOCAL);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRoles(resolveRoles(roleNames));
        return userRepository.save(u);
    }

    @Transactional
    public User upsertGoogleUser(String email, String sub, String givenName, String familyName) {
        var existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            existing.setProvider(AuthProvider.GOOGLE);
            existing.setGoogleSub(sub);
            if (existing.getFirstName() == null && givenName != null) existing.setFirstName(givenName);
            if (existing.getLastName() == null && familyName != null) existing.setLastName(familyName);
            if (existing.getUsername() == null) existing.setUsername(email);
            if (existing.getRoles() == null || existing.getRoles().isEmpty()) {
                existing.setRoles(defaultRoles());
            }
            return userRepository.save(existing);
        }
        User u = new User();
        u.setEmail(email);
        u.setUsername(email);
        u.setProvider(AuthProvider.GOOGLE);
        u.setGoogleSub(sub);
        u.setFirstName(givenName);
        u.setLastName(familyName);
        u.setRoles(defaultRoles());
        return userRepository.save(u);
    }

    private Set<Role> resolveRoles(Set<String> names) {
        if (names == null || names.isEmpty()) return defaultRoles();
        Set<Role> roles = new HashSet<>();
        for (String n : names) {
            String name = n.startsWith("ROLE_") ? n : "ROLE_" + n;
            var role = roleRepository.findByName(name)
                    .orElseGet(() -> roleRepository.save(new Role(name)));
            roles.add(role);
        }
        return roles;
    }

    private Set<Role> defaultRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(new Role("ROLE_USER"))));
        return roles;
    }
}
