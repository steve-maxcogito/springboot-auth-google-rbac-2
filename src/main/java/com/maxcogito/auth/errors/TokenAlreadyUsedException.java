package com.maxcogito.auth.errors;

public class TokenAlreadyUsedException extends RuntimeException {
    public TokenAlreadyUsedException() {
        super("Token already used");
    }
}
