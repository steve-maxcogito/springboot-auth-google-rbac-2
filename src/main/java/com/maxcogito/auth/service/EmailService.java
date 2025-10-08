package com.maxcogito.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final GraphMailService graph;

    public EmailService(GraphMailService graph) {
        this.graph = graph;
    }

    public void send(String to, String subject, String bodyPlainOrHtml) {
        String html = bodyPlainOrHtml.contains("<")
                ? bodyPlainOrHtml
                : "<html><body><pre style=\"font-family:inherit\">" +
                escape(bodyPlainOrHtml) + "</pre></body></html>";
        graph.sendHtml(to, subject, html);
    }

    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
