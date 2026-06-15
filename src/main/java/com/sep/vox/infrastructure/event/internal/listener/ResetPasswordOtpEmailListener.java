package com.sep.vox.infrastructure.event.internal.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sep.vox.application.event.SendResetPasswordOtpEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;

@Component
public class ResetPasswordOtpEmailListener {

    private static final String SUBJECT = "Mã xác thực đặt lại mật khẩu VOX";
    private static final String EXPIRES_IN = "5 phút";

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public ResetPasswordOtpEmailListener(MailSendingPort mailSendingPort, MailTemplatePort mailTemplatePort) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }
    
    @EventListener
    public void handle(SendResetPasswordOtpEvent event) throws Exception {
        var html = mailTemplatePort.renderResetPasswordOtpEmail(event.otp(), EXPIRES_IN);
        mailSendingPort.sendHtml(event.to(), SUBJECT, html);
    }
}
