package com.sep.vox.infrastructure.event.internal.listener;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.ExamAppealPublishedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.repository.UserRepository;

@Component
public class ExamAppealPublishedEmailListener {

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public ExamAppealPublishedEmailListener(
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort) {
        this.userRepository = userRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExamAppealPublishedEvent event) throws Exception {
        var student = userRepository.findById(event.studentId()).orElse(null);
        if (student == null) {
            return;
        }
        var html = mailTemplatePort.renderAppealPublishedEmail(
            event.examName(),
            format(event.scoreBefore()),
            format(event.scoreAfter())
        );
        mailSendingPort.sendHtml(student.getEmail().value(), "Kết quả phúc khảo đã được công bố", html);
    }

    private String format(BigDecimal score) {
        return score == null ? "-" : score.toPlainString();
    }
}
