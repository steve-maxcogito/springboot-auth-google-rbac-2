package com.maxcogito.auth.controller;

import com.maxcogito.auth.errors.InvalidTokenException;
import com.maxcogito.auth.errors.TokenAlreadyUsedException;
import com.maxcogito.auth.errors.TokenExpiredException;
import com.maxcogito.auth.service.VerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class VerificationController {
    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);
    @Value("${app.frontendBaseUrl}")
    private String frontend; // e.g., http://localhost:5173 or https://app.maxcogito.com

    @GetMapping("/verify/confirmCode")
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

    @GetMapping("/verify/confirmLink")
    public ResponseEntity<Void> confirmVerifyLinkCode(@RequestParam("token") String token) {
        try {
            // use your method name: confirm(...) or confirmCode(...)
            log.info("Received confirm link code: {}", token);
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
