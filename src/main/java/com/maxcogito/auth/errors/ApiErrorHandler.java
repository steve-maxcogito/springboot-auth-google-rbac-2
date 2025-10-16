package com.maxcogito.auth.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail handleInvalid(InvalidTokenException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid verification token");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "VERIFICATION_INVALID");
        return pd; // -> 400 with RFC7807 body
    }

    @ExceptionHandler(TokenExpiredException.class)
    ProblemDetail handleExpired(TokenExpiredException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.GONE); // 410 Gone
        pd.setTitle("Verification token expired");
        pd.setDetail("Your link has expired. Please request a new one.");
        pd.setProperty("code", "VERIFICATION_EXPIRED");
        return pd;
    }

    @ExceptionHandler(TokenAlreadyUsedException.class)
    ProblemDetail handleUsed(TokenAlreadyUsedException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT); // 409
        pd.setTitle("Verification token already used");
        pd.setDetail("This link was already used. You can sign in now.");
        pd.setProperty("code", "VERIFICATION_ALREADY_USED");
        return pd;
    }
}

