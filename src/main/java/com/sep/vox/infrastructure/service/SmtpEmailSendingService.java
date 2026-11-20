package com.sep.vox.infrastructure.service;

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
 * Gửi mail qua SMTP.
 *
 * <p>Địa chỉ người gửi được set TƯỜNG MINH ở cả hai method. Không set thì JavaMail tự suy địa chỉ
 * local từ user.name + hostname của máy đang chạy, và khi không suy được (rất hay gặp trong
 * container) nó ném "can't determine local email address" -- mọi mail đều không gửi được.
 *
 * <p>CỐ TÌNH không dùng @Async: nơi duy nhất gọi port này là các Kafka consumer, mà container của
 * chúng đã lo concurrency còn @RetryableTopic đã lo retry/DLT. Chạy bất đồng bộ ở đây khiến lời gọi
 * trả về trước khi SMTP chạy, nên try/catch phía consumer không bắt được gì, event vẫn được
 * markProcessed + ack, và mail hỏng mất luôn không retry.
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
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }

    @Override
    public void sendHtml(String to, String subject, String html) throws Exception {
        var message = javaMailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(fromAddress());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        javaMailSender.send(message);
    }

    /**
     * Tên hiển thị chỉ là phần trang trí -- mã hoá hỏng thì vẫn phải gửi được mail, nên rơi về
     * địa chỉ trần thay vì ném.
     */
    private InternetAddress fromAddress() throws AddressException, UnsupportedEncodingException {
        if (fromDisplayName == null || fromDisplayName.isBlank()) {
            return new InternetAddress(from);
        }
        return new InternetAddress(from, fromDisplayName, StandardCharsets.UTF_8.name());
    }
}
