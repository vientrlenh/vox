package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentAppealRejectedPayload;
import com.sep.vox.application.port.input.command.RejectAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

@Service
public class RejectAssessmentAppealUseCase implements IUseCase<RejectAssessmentAppealCommand, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public RejectAssessmentAppealUseCase(
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentAppealAccessService assessmentAppealAccessService,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public UUID execute(RejectAssessmentAppealCommand command) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối đơn phúc khảo đang ở trạng thái Chờ duyệt.");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("Phải nêu lý do từ chối đơn phúc khảo.");
        }

        var now = Instant.now();
        appeal.setStatus(AssessmentAppealStatus.REJECTED);
        appeal.setDecisionNote(command.reason());
        appeal.setResolvedBy(currentUserId);
        appeal.setResolvedAt(now);
        assessmentResultAppealRepository.save(appeal);

        // Đơn bị từ chối: điểm gốc giữ nguyên giá trị, kết quả quay về đã công bố.
        var candidateResult = context.candidateResult();
        candidateResult.setStatus(AssessmentCandidateResultStatus.RELEASED);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        assessmentCandidateResultRepository.save(candidateResult);

        // Ghi outbox trong chính transaction này: mail báo học sinh và quyết định từ chối
        // cùng sống hoặc cùng chết, không còn cảnh commit xong rồi mất mail vì SMTP lỗi.
        var payload = new AssessmentAppealRejectedPayload(
            appeal.getId(),
            context.studentId(),
            context.assessmentName(),
            command.reason()
        );
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.ASSESSMENT_APPEAL_REJECTED,
            jsonSerializationPort.toJson(payload),
            now
        ));

        return appeal.getId();
    }
}
