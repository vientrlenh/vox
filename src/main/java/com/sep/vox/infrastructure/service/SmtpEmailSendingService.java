package com.sep.vox.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.MailSendingPort;

import jakarta.mail.MessagingException;



@Service
public class SmtpEmailSendingService implements MailSendingPort {

    private final JavaMailSender javaMailSender;

    public SmtpEmailSendingService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailSendingService.class);

    @Override
    @Async("mailExecutor")
    public void send(String to, String subject, String body) {
        try {
            var message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            javaMailSender.send(message);
        } catch (MailException e) {
            LOGGER.error("Email cannot be sent properly to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String html) {
        try {
            var message = javaMailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (MessagingException | MailException e) {
            LOGGER.error("Email cannot be sent properly to {}: {}", to, e.getMessage());
        }
    }

    
}
