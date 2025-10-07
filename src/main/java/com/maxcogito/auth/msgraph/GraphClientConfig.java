package com.maxcogito.auth.msgraph;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class GraphClientConfig {
    @Value("${msgraph.tenantId}")     private String tenantId;
    @Value("${msgraph.clientId}")     private String clientId;
    @Value("${msgraph.clientSecret}") private String clientSecret;

    @Bean
    public GraphServiceClient graphServiceClient() {
        TokenCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        // v6: pass credential + scopes directly
        return new GraphServiceClient((AuthenticationProvider) credential, (OkHttpClient) List.of("https://graph.microsoft.com/.default"));
    }
}
