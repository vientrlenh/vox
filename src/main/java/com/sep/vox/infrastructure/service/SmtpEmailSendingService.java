package com.sep.vox.infrastructure.service;

import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.MailSendingPort;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * Sends mail over SMTP.
 *
 * <p>The sender address is set EXPLICITLY in both methods. If not set, JavaMail derives a local
 * address from user.name + the hostname of the machine it runs on, and when it cannot (very common in
 * containers) it throws "can't determine local email address" -- no mail can be sent at all.
 *
 * <p>INTENTIONALLY not @Async: the only callers of this port are the Kafka consumers, whose container
 * already handles concurrency while @RetryableTopic already handles retry/DLT. Running asynchronously here makes the call
 * return before SMTP runs, so the consumer-side try/catch catches nothing, the event is still
 * markProcessed + acked, and a failed mail is lost with no retry.
 */
@Service
public class SmtpEmailSendingService implements MailSendingPort {

    private final JavaMailSender javaMailSender;
    private final String from;
    private final String fromDisplayName;

    public SmtpEmailSendingService(
            JavaMailSender javaMailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.from-display-name}") String fromDisplayName) {
        this.javaMailSender = javaMailSender;
        this.from = from;
        this.fromDisplayName = fromDisplayName;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }

    @Override
    public void sendHtml(String to, String subject, String html) throws Exception {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(fromAddress());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        javaMailSender.send(message);
    }

    /**
     * The display name is only decoration -- if encoding fails the mail must still be sent, so it falls back to
     * the bare address instead of throwing.
     */
    private InternetAddress fromAddress() throws AddressException, UnsupportedEncodingException {
        if (fromDisplayName == null || fromDisplayName.isBlank()) {
            return new InternetAddress(from);
        }
        return new InternetAddress(from, fromDisplayName, StandardCharsets.UTF_8.name());
    }
}
