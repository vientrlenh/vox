package com.sep.vox.infrastructure.event.internal.listener;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.ExamBlueprintVersionPublishedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.application.query.repository.SchoolUserQueryRepository;

@Component
public class ExamBlueprintVersionPublishedEmailListener {

    private final SchoolUserQueryRepository schoolUserQueryRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public ExamBlueprintVersionPublishedEmailListener(
            SchoolUserQueryRepository schoolUserQueryRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort) {
        this.schoolUserQueryRepository = schoolUserQueryRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExamBlueprintVersionPublishedEvent event) throws Exception {
        var html = mailTemplatePort.renderExamBlueprintReadyEmail(event.blueprintName(), event.blueprintCode());
        var schoolAdmins = schoolUserQueryRepository.findBySchoolIdAndRoleCodes(
            event.schoolId(),
            List.of("SCHOOL_ADMIN"),
            0,
            50
        );
        for (var admin : schoolAdmins.content()) {
            mailSendingPort.sendHtml(admin.email(), "Blueprint đề thi đã sẵn sàng", html);
        }
    }
}
