package com.maxcogito.auth.service;

import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class GraphMailService {

    private final GraphServiceClient graph;
    private final String senderUpnOrId;

    public GraphMailService(GraphServiceClient graph,
                            @Value("${msgraph.senderAddress}") String senderUpnOrId) {
        this.graph = graph;
        this.senderUpnOrId = senderUpnOrId; // UPN/email of mailbox or its GUID
    }

    public void sendHtml(String to, String subject, String html) {
        // Build message via setters (v6)
        Message message = new Message();
        message.setSubject(subject);

        ItemBody body = new ItemBody();
        // If your enum shows BodyType.Text instead of TEXT, use that constant.
        body.setContentType(BodyType.Html);
        body.setContent(html);
        message.setBody(body);

        EmailAddress addr = new EmailAddress();
        addr.setAddress(to);
        Recipient rcpt = new Recipient();
        rcpt.setEmailAddress(addr);
        message.setToRecipients(List.of(rcpt));

        SendMailPostRequestBody req = new SendMailPostRequestBody();
        req.setMessage(message);
        req.setSaveToSentItems(Boolean.TRUE);

        // App-only: send as the specific mailbox
        graph.users().byUserId(senderUpnOrId).sendMail().post(req);
    }
}
