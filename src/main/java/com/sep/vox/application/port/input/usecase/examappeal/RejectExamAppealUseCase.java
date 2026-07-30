package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAppealRejectedEvent;
import com.sep.vox.application.port.input.command.RejectExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

@Service
public class RejectExamAppealUseCase implements IUseCase<RejectExamAppealCommand, UUID> {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final EventPublisherPort eventPublisherPort;

    public RejectExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamAppealAccessService examAppealAccessService,
            EventPublisherPort eventPublisherPort) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public UUID execute(RejectExamAppealCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdmin(context, currentUserId);

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

        eventPublisherPort.publish(new ExamAppealRejectedEvent(
            appeal.getId(),
            context.studentId(),
            context.examName(),
            command.reason()
        ));

        return appeal.getId();
    }
}
