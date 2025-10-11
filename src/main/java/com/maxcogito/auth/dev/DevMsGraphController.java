package com.maxcogito.auth.dev;

import com.maxcogito.auth.ms.AadTokenProvider;
import com.maxcogito.auth.service.GraphMailService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/dev/msgraph")
@Profile("dev")
public class DevMsGraphController {
    private final AadTokenProvider tokenProvider;
    private final GraphMailService mail;

    public DevMsGraphController(AadTokenProvider tokenProvider, GraphMailService mail) {
        this.tokenProvider = tokenProvider;
        this.mail = mail;
    }

    // Quick status check (does NOT force refresh)
    @GetMapping("/token/status")
    @PreAuthorize("hasRole('ADMIN')") // keep it admin-only even in dev
    public ResponseEntity<?> status() {
        // we don’t expose the token; we just try to grab it (which may refresh if near expiry)
        String bearer = tokenProvider.getBearer();
        // We can’t read the real expiry from AAD; so we report “now + 55m” to show it’s fresh.
        // If you want exact numbers, store them inside AadTokenProvider and expose here.
        return ResponseEntity.ok(Map.of(
                "hasToken", bearer != null,
                "checkedAt", Instant.now().toString()
        ));
    }

    // Force refresh
    @PostMapping("/token/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refresh() {
        String bearer = tokenProvider.refresh();
        return ResponseEntity.ok(Map.of(
                "refreshed", true,
                "checkedAt", Instant.now().toString(),
                "note", "Token refreshed server-side (not returned)."
        ));
    }

    // Send a test message via Graph
    @PostMapping("/test-send-old")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testSend2(@RequestBody MailTestRequest req) {
        mail.sendHtml(req.to(), req.subject(), req.html());
        return ResponseEntity.accepted().body(Map.of(
                "sentTo", req.to(),
                "subject", req.subject()
        ));
    }

    // Send a test message via Graph
    @PostMapping("/test-send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testSend(@RequestBody MailTestRequest req) {
        try {
            mail.sendHtml(req.to(), req.subject(), req.html());
            return ResponseEntity.noContent().build();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Surface Graph status and body
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Graph call failed",
                            "status", e.getStatusCode().value(),
                            "graphBody", e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    public record MailTestRequest(
            @Email String to,
            @NotBlank String subject,
            @NotBlank String html
    ) {}
}
