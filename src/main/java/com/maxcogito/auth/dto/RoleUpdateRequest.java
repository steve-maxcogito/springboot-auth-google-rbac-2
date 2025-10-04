package com.maxcogito.auth.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RoleUpdateRequest(@NotEmpty Set<String> roles) {
}
