package com.maxcogito.auth.service;


import com.maxcogito.auth.ms.AadTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.List;
import java.util.Map;

@Service
public class GraphMailService {

    private final RestTemplate rest = new RestTemplate();
    private final AadTokenProvider tokenProvider;

    @Value("${msgraph.senderAddress}")
    private String senderAddress; // e.g. noreply@yourdomain.com

    public GraphMailService(AadTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void sendHtml(String to, String subject, String html) {
        var url = "https://graph.microsoft.com/v1.0/users/" + senderAddress + "/sendMail";
        var body = Map.of(
                "message", Map.of(
                        "subject", subject,
                        "body", Map.of(
                                "contentType", "HTML",
                                "content", html
                        ),
                        "toRecipients", new Object[] {
                                Map.of("emailAddress", Map.of("address", to))
                        }
                ),
                "saveToSentItems", Boolean.FALSE
        );

        doSend(url, body, /*retryIfUnauthorized=*/true);
    }

    private void doSend(String url, Object payload, boolean retry) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(tokenProvider.getBearer());

        try {
            rest.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, h), Void.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (retry) {
                tokenProvider.refresh(); // force refresh
                doSend(url, payload, false);
            } else {
                throw e;
            }
        }
    }

}
