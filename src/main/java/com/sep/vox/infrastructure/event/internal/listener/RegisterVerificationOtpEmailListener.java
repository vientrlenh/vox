package com.sep.vox.infrastructure.event.internal.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.SendRegisterVerificationOtpEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;

@Component
public class RegisterVerificationOtpEmailListener {

    private static final String SUBJECT = "Mã xác thực đăng ký VOX";
    private static final String EXPIRES_IN = "10 phút";

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public RegisterVerificationOtpEmailListener(MailSendingPort mailSendingPort, MailTemplatePort mailTemplatePort) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SendRegisterVerificationOtpEvent event) throws Exception {
        var html = mailTemplatePort.renderRegisterVerificationOtpEmail(event.otp(), EXPIRES_IN);
        mailSendingPort.sendHtml(event.to(), SUBJECT, html);
    }
}
