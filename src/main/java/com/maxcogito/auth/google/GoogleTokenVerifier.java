package com.maxcogito.auth.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GoogleTokenVerifier {

    private final List<String> clientIds;

    public GoogleTokenVerifier(@Value("${google.clientIds}") List<String> clientIds) {
        this.clientIds = clientIds;
    }

    public GoogleIdToken.Payload verify(String idToken) throws Exception {
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(clientIds != null ? clientIds : Collections.emptyList())
                .build();

        GoogleIdToken token = verifier.verify(idToken);
        if (token == null) return null;
        return token.getPayload();
    }
}
