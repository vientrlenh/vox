package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UnblockAssessmentCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class UnblockAssessmentCandidateUseCase implements IUseCase<UnblockAssessmentCandidateCommand, AssessmentCandidateDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnblockAssessmentCandidateUseCase.class);

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;

    public UnblockAssessmentCandidateUseCase(
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentItemAnswerRepository assessmentItemAnswerRepository,
            AssessmentItemEvaluationRepository assessmentItemEvaluationRepository,
            UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase,
            AssessmentSessionModerationAccessService moderationAccessService,
            ClassTestGradingAssignmentService classTestGradingAssignmentService) {
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
        this.assessmentItemEvaluationRepository = assessmentItemEvaluationRepository;
        this.upsertAssessmentCandidateResultUseCase = upsertAssessmentCandidateResultUseCase;
        this.moderationAccessService = moderationAccessService;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
    }

    @Override
    @Transactional
    public AssessmentCandidateDto execute(UnblockAssessmentCandidateCommand input) {
        var candidate = assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh"));
        var assessment = assessmentRepository.findById(candidate.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của thí sinh"));

        moderationAccessService.authorize(assessment, candidate);
        // G.4: mốc chặn cứng duy nhất là RESULTS_PUBLISHED - trước đó vẫn dỡ cấm được,
        // kể cả sau khi kỳ thi đã đóng (đang trong giai đoạn rà soát trước công bố).
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố kết quả, không thể thay đổi nữa");
        }

        var now = Instant.now();
        candidate.setBlockedAt(null);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(moderationAccessService.getCurrentUserId());
        var saved = assessmentCandidateRepository.save(candidate);

        recomputeInvalidatedSessions(candidate.getId());
        return AssessmentCandidateDto.toDto(saved);
    }

    /**
     * G.4 case 2: soi lại thấy KHÔNG vi phạm - tính điểm lại cho mọi session của thí
     * sinh đang bị INVALID do từng bị đánh dấu vi phạm oan. Đã từng có AssessmentItemEvaluation
     * -> recompute từ dữ liệu cũ (ưu tiên điểm con người, không gọi AI lại); chưa từng có
     * -> chuyển PENDING_REVIEW cho giáo viên chấm tay, KHÔNG gọi AI (xem
     * {@link #moveToPendingReviewWithoutScores}).
     */
    private void recomputeInvalidatedSessions(java.util.UUID candidateId) {
        for (var session : assessmentSessionRepository.findByCandidateId(candidateId)) {
            var result = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
            if (result == null || result.getStatus() != AssessmentCandidateResultStatus.INVALID) {
                continue;
            }

            var responseIds = assessmentItemAnswerRepository.findBySessionId(session.getId()).stream()
                .map(response -> response.getId())
                .toList();
            var hasEvaluations = !assessmentItemEvaluationRepository.findByResponseIdIn(responseIds).isEmpty();
            if (hasEvaluations) {
                var recalculated = upsertAssessmentCandidateResultUseCase.execute(session.getId());
                // Gỡ chặn có thể kéo bài từ INVALID về PENDING_REVIEW — bài trên lớp cần
                // một phân công mới thì giáo viên chủ bài mới chấm lại được.
                classTestGradingAssignmentService.ensureAssignmentForResult(recalculated);
            } else {
                moveToPendingReviewWithoutScores(result, session.getId());
            }
        }
    }

    /**
     * Bài CHƯA từng được chấm câu nào: chuyển thẳng sang {@code PENDING_REVIEW} cho giáo viên
     * chấm tay, KHÔNG gọi AI.
     *
     * <p><strong>Vì sao bỏ lời gọi AI (2026-08-16):</strong> bản trước chạy
     * {@code retryGradingAssessmentSession} ở nhánh này. Nhưng bài rơi vào đây là bài bị buộc kết
     * thúc giữa chừng — thí sinh mới nói được một hai câu, thường còn dở câu. Bắt AI chấm một
     * bài như vậy vừa tốn một lượt gọi, vừa cho ra điểm không có nghĩa, trong khi thứ giáo viên
     * cần để phán quyết là bản ghi lời nói chứ không phải điểm máy. Bản ghi đó vẫn còn nguyên
     * trong {@code assessment_item_answer_turns} và nay đã hiện được ở màn chấm
     * (JpaAssessmentGradingQueryRepository#turnsForItem).
     *
     * <p><strong>Vì sao KHÔNG đi qua {@code UpsertAssessmentCandidateResultUseCase}:</strong> nó gọi
     * {@code AssessmentSessionResultCalculator.calculate}, mà hàm đó ném {@code ResourceNotFoundException}
     * ngay khi một câu trả lời thiếu evaluation (dòng 82-85). Không có evaluation nào thì không
     * có gì để tính — đặt thẳng trạng thái là cách đúng, thay vì nới một hàm dùng chung cho mọi
     * đường tính điểm chỉ để phục vụ ca này.
     *
     * <p>Điểm giữ nguyên (chưa có điểm nào để mà đổi); giáo viên chấm xong thì chính lần chấm
     * đó sinh evaluation, và từ đó mọi đường tính điểm thông thường chạy lại bình thường.
     */
    private void moveToPendingReviewWithoutScores(
            AssessmentCandidateResult result, UUID sessionId) {
        var now = Instant.now();
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        result.setUpdatedAt(now);
        result.setUpdatedBy(moderationAccessService.getCurrentUserId());
        assessmentCandidateResultRepository.save(result);
        LOGGER.info(
            "Gỡ chặn phiên {}: chưa có evaluation nào, chuyển PENDING_REVIEW cho giáo viên chấm tay (không gọi AI).",
            sessionId
        );
    }
}
