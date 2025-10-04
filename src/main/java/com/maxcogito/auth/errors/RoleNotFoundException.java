package com.maxcogito.auth.errors;

public class RoleNotFoundException extends RuntimeException{
    public RoleNotFoundException(String roleName) {
        super(roleName);
    }
}
