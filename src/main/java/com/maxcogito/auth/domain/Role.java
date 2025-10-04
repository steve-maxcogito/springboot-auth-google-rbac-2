package com.maxcogito.auth.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "role", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Role {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., ROLE_USER, ROLE_ADMIN, ROLE_SECURITY_SERVICE, ROLE_DATA_SERVICE

    public Role() {}
    public Role(String name) { this.name = name; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
