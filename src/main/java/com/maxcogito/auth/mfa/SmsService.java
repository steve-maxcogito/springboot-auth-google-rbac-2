package com.maxcogito.auth.mfa;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.fromNumber}")
    private String fromNumber;

    @PostConstruct
    void init() {
        Twilio.init(accountSid, authToken);
    }

    public void send(String toNumberE164, String body) {
        Message.creator(new com.twilio.type.PhoneNumber(toNumberE164),
                new com.twilio.type.PhoneNumber(fromNumber),
                body).create();
    }
}
