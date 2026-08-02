package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.port.input.command.RejectExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

@Service
public class RejectExamAppealUseCase implements IUseCase<RejectExamAppealCommand, UUID> {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public RejectExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamAppealAccessService examAppealAccessService,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public UUID execute(RejectExamAppealCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối đơn phúc khảo đang ở trạng thái Chờ duyệt.");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("Phải nêu lý do từ chối đơn phúc khảo.");
        }

        var now = Instant.now();
        appeal.setStatus(ExamAppealStatus.REJECTED);
        appeal.setDecisionNote(command.reason());
        appeal.setResolvedBy(currentUserId);
        appeal.setResolvedAt(now);
        examResultAppealRepository.save(appeal);

        // Đơn bị từ chối: điểm gốc giữ nguyên giá trị, kết quả quay về đã công bố.
        var candidateResult = context.candidateResult();
        candidateResult.setStatus(ExamCandidateResultStatus.RELEASED);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        examCandidateResultRepository.save(candidateResult);

        // Ghi outbox trong chính transaction này: mail báo học sinh và quyết định từ chối
        // cùng sống hoặc cùng chết, không còn cảnh commit xong rồi mất mail vì SMTP lỗi.
        var payload = new ExamAppealRejectedPayloadV1(
            appeal.getId(),
            context.studentId(),
            context.examName(),
            command.reason()
        );
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.EXAM_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.EXAM_APPEAL_REJECTED,
            jsonSerializationPort.toJson(payload),
            now
        ));

        return appeal.getId();
    }
}
