package com.sep.vox.infrastructure.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.MailSendingPort;



@Service
public class SmtpEmailSendingService implements MailSendingPort {

    private final JavaMailSender javaMailSender;

    public SmtpEmailSendingService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    @Async("mailExecutor")
    public void send(String to, String subject, String body) {
        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }

    @Override
    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String html) throws Exception {
        var message = javaMailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        javaMailSender.send(message);
    }

    
}
