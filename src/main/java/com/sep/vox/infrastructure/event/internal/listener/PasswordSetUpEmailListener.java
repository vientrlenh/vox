package com.sep.vox.infrastructure.event.internal.listener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.PasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;

@Component
public class PasswordSetUpEmailListener {
    
    private static final String SUBJECT = "Thiết lập mật khẩu tài khoản VOX";
    private static final String EXPIRES_IN = "48 giờ";

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    
    public PasswordSetUpEmailListener(MailSendingPort mailSendingPort, MailTemplatePort mailTemplatePort) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @Value("${app.frontend.password-setup-url}")
    private String passwordSetupBaseUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordSetUpEmailRequestedEvent event) throws Exception {
        var setupUrl = buildPasswordSetupUrl(event.userId(), event.rawToken());
        var html = mailTemplatePort.renderPasswordSetUpEmail(
            event.schoolAdminName(), 
            event.schoolName(), 
            setupUrl, 
            EXPIRES_IN
        );

        mailSendingPort.sendHtml(event.to(), SUBJECT, html);
    }

    private String buildPasswordSetupUrl(UUID userId, String rawToken) {
        var separator = passwordSetupBaseUrl.contains("?") ? "&" : "?";
        return passwordSetupBaseUrl + separator + "userId=" + encode(userId.toString()) + "&token=" + encode(rawToken);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
