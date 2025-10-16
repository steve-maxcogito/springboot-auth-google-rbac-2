package com.maxcogito.auth.controller;

import com.maxcogito.auth.errors.InvalidTokenException;
import com.maxcogito.auth.errors.TokenAlreadyUsedException;
import com.maxcogito.auth.errors.TokenExpiredException;
import com.maxcogito.auth.service.VerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class VerificationController {
    @Value("${app.frontendBaseUrl}")
    private String frontend; // e.g., http://localhost:5173 or https://app.maxcogito.com

    @GetMapping("/api/auth/verify/confirmCode")
    public ResponseEntity<Void> confirmVerifyCode(@RequestParam("token") String token) {
        try {
            // use your method name: confirm(...) or confirmCode(...)
            verificationService.confirmCode(token); // sets used=true and emailVerified=true
            return redirect(frontend + "/verify/success");      // first click
        } catch (TokenAlreadyUsedException e) {
            return redirect(frontend + "/verify/success");      // second+ click -> still success
        } catch (TokenExpiredException e) {
            return redirect(frontend + "/verify/expired");
        } catch (InvalidTokenException e) {
            return redirect(frontend + "/verify/invalid");
        }
    }

    @GetMapping("/api/auth/verify/confirmLink")
    public ResponseEntity<Void> confirmVerifyLinkCode(@RequestParam("token") String token) {
        try {
            // use your method name: confirm(...) or confirmCode(...)
            verificationService.confirmVerifyLink(token); // sets used=true and emailVerified=true
            return redirect(frontend + "/verify/success");      // first click
        } catch (TokenAlreadyUsedException e) {
            return redirect(frontend + "/verify/success");      // second+ click -> still success
        } catch (TokenExpiredException e) {
            return redirect(frontend + "/verify/expired");
        } catch (InvalidTokenException e) {
            return redirect(frontend + "/verify/invalid");
        }
    }

    private static ResponseEntity<Void> redirect(String location) {
        // 303 See Other is best for redirect-after-GET/POST
        return ResponseEntity.status(303).location(URI.create(location)).build();
    }

    // inject your VerificationService (constructor omitted for brevity)
    private final VerificationService verificationService;
    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }
}
