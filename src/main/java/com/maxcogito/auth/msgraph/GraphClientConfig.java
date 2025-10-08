package com.maxcogito.auth.msgraph;


import com.microsoft.kiota.http.OkHttpRequestAdapter;
import com.microsoft.kiota.serialization.ParseNodeFactory;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AzureIdentityAuthenticationProvider;
import com.microsoft.kiota.serialization.JsonParseNodeFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

        // Kiota auth provider using Azure Identity (scope goes here)


      //  var authProvider = new AzureIdentityAccessTokenProvider(
       //         credential,
        //        new String[] { "https://graph.microsoft.com/.default" }
      //  );


        final AzureIdentityAuthenticationProvider authProvider =
                new AzureIdentityAuthenticationProvider(
                        credential,
                        new String[] { "graph.microsoft.com" }, // allowed hosts
                        new String[] { "https://graph.microsoft.com/.default" } // scopes
                );

        ParseNodeFactory parseNodeFactory = new JsonParseNodeFactory();

        // OkHttp adapter (you can reuse/share the OkHttpClient if you like)
        var adapter = new OkHttpRequestAdapter(authProvider, parseNodeFactory);

        // v6 Graph client takes a RequestAdapter
        return new GraphServiceClient(adapter);
    }
}
