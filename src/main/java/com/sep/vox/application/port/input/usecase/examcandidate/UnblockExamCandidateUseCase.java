package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RetryGradingExamSessionCommand;
import com.sep.vox.application.port.input.command.UnblockExamCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examsession.RetryGradingExamSessionUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class UnblockExamCandidateUseCase implements IUseCase<UnblockExamCandidateCommand, ExamCandidateDto> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private final RetryGradingExamSessionUseCase retryGradingExamSessionUseCase;
    private final ExamSessionModerationAccessService moderationAccessService;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;

    public UnblockExamCandidateUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase,
            RetryGradingExamSessionUseCase retryGradingExamSessionUseCase,
            ExamSessionModerationAccessService moderationAccessService,
            ClassTestGradingAssignmentService classTestGradingAssignmentService) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
        this.retryGradingExamSessionUseCase = retryGradingExamSessionUseCase;
        this.moderationAccessService = moderationAccessService;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(UnblockExamCandidateCommand input) {
        var candidate = examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        var exam = examRepository.findById(candidate.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của thí sinh"));

        moderationAccessService.authorize(exam, candidate);
        // G.4: mốc chặn cứng duy nhất là RESULTS_PUBLISHED - trước đó vẫn dỡ cấm được,
        // kể cả sau khi kỳ thi đã đóng (đang trong giai đoạn rà soát trước công bố).
        if (exam.getStatus() == ExamStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố kết quả, không thể thay đổi nữa");
        }

        var now = Instant.now();
        candidate.setBlockedAt(null);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(moderationAccessService.getCurrentUserId());
        var saved = examCandidateRepository.save(candidate);

        recomputeInvalidatedSessions(candidate.getId());
        return ExamCandidateDtoMapper.toDto(saved);
    }

    /**
     * G.4 case 2: soi lại thấy KHÔNG vi phạm - tính điểm lại cho mọi session của thí
     * sinh đang bị INVALID do từng bị đánh dấu vi phạm oan. Đã từng có ExamItemEvaluation
     * -> recompute từ dữ liệu cũ (ưu tiên điểm con người, không gọi AI lại); chưa từng có
     * -> AI chấm thật lần đầu qua retryGradingExamSession (đã mở rộng để nhận case này).
     */
    private void recomputeInvalidatedSessions(java.util.UUID candidateId) {
        for (var session : examSessionRepository.findAllByCandidateId(candidateId)) {
            var result = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
            if (result == null || result.getStatus() != ExamCandidateResultStatus.INVALID) {
                continue;
            }

            var responseIds = examItemResponseRepository.findBySessionId(session.getId()).stream()
                .map(response -> response.getId())
                .toList();
            var hasEvaluations = !examItemEvaluationRepository.findByResponseIdIn(responseIds).isEmpty();
            if (hasEvaluations) {
                var recalculated = upsertExamCandidateResultUseCase.execute(session.getId());
                // Gỡ chặn có thể kéo bài từ INVALID về PENDING_REVIEW — bài trên lớp cần
                // một phân công mới thì giáo viên chủ bài mới chấm lại được.
                classTestGradingAssignmentService.ensureAssignmentForResult(recalculated);
            } else {
                retryGradingExamSessionUseCase.execute(new RetryGradingExamSessionCommand(session.getId()));
            }
        }
    }
}
