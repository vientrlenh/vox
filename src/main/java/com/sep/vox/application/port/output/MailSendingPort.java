package com.sep.vox.application.port.output;

public interface MailSendingPort {
    void send(String to, String subject, String body);
    void sendHtml(String to, String subject, String html) throws Exception;
}
