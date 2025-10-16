package com.maxcogito.auth.errors;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token expired");
    }
}
