package com.sep.vox.infrastructure.event.internal.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.ExamAppealRejectedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.repository.UserRepository;

@Component
public class ExamAppealRejectedEmailListener {

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public ExamAppealRejectedEmailListener(
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort) {
        this.userRepository = userRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExamAppealRejectedEvent event) throws Exception {
        var student = userRepository.findById(event.studentId()).orElse(null);
        if (student == null) {
            return;
        }
        var html = mailTemplatePort.renderAppealRejectedEmail(event.examName(), event.reason());
        mailSendingPort.sendHtml(student.getEmail().value(), "Đơn phúc khảo không được chấp nhận", html);
    }
}
