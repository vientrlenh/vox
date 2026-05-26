package com.sep.vox.infrastructure.event.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.RegisterFormRejectedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;

@Component
public class RejectRegisterFormEmailListener {

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public RejectRegisterFormEmailListener(MailSendingPort mailSendingPort, MailTemplatePort mailTemplatePort) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RegisterFormRejectedEvent event) throws Exception {
        var html = mailTemplatePort.renderRejectRegisterFormEmail(event.reason());
        mailSendingPort.sendHtml(event.to(), "Thông báo kết quả đăng ký VOX", html);
    }
}
