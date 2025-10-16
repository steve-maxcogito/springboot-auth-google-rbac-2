package com.maxcogito.auth.errors;

public class InvalidTokenException extends RuntimeException{
    public InvalidTokenException() {
        super("Invalid token");
    }
}
