package com.nguyendin.operationservice.dto;

import java.util.Map;

/**
 * DTO for sending email requests
 */
public class SendEmailRequest {
    private String to;
    private String subject;
    private String body;
    private Map<String, String> variables;

    public SendEmailRequest() {
    }

    public SendEmailRequest(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public SendEmailRequest(String to, String subject, String body, Map<String, String> variables) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.variables = variables;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
}

