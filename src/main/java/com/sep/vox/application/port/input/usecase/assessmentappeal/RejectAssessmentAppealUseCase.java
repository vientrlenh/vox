package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentAppealRejectedPayloadV1;
import com.sep.vox.application.port.input.command.assessmentappeal.RejectAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

@Service
@RequiredArgsConstructor
public class RejectAssessmentAppealUseCase implements IUseCase<RejectAssessmentAppealCommand, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public UUID execute(RejectAssessmentAppealCommand command) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        AssessmentResultAppeal appeal = context.appeal();
        requireRejectable(appeal, command);

        Instant now = Instant.now();
        appeal.setStatus(AssessmentAppealStatus.REJECTED);
        appeal.setDecisionNote(command.reason());
        appeal.setResolvedBy(currentUserId);
        appeal.setResolvedAt(now);
        assessmentResultAppealRepository.save(appeal);
        releaseCandidateResult(context.candidateResult());
        saveAppealRejectedOutbox(appeal, context, command.reason(), now);

        return appeal.getId();
    }

    private void requireRejectable(AssessmentResultAppeal appeal, RejectAssessmentAppealCommand command) {
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể từ chối đơn phúc khảo đang ở trạng thái Chờ duyệt.");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("Phải nêu lý do từ chối đơn phúc khảo.");
        }
    }

    /** A rejected appeal leaves the original score unchanged and the result goes back to released. */
    private void releaseCandidateResult(AssessmentCandidateResult candidateResult) {
        candidateResult.setStatus(AssessmentCandidateResultStatus.RELEASED);
        assessmentCandidateResultRepository.save(candidateResult);
    }

    /**
     * The outbox row is written inside this transaction so the mail to the student and the rejection live or die
     * together: no more committing and then losing the mail to an SMTP failure.
     */
    private void saveAppealRejectedOutbox(
        AssessmentResultAppeal appeal, AssessmentAppealAccessService.AppealContext context, String reason, Instant now) {
        AssessmentAppealRejectedPayloadV1 payload = new AssessmentAppealRejectedPayloadV1(
            appeal.getId(),
            context.studentId(),
            context.assessmentName(),
            reason
        );
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.ASSESSMENT_APPEAL_REJECTED,
            jsonSerializationPort.toJson(payload),
            now
        ));
    }
}
