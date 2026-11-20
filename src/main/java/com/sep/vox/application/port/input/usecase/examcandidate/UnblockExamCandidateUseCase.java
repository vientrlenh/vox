package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UnblockExamCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(UnblockExamCandidateUseCase.class);

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
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
            ExamSessionModerationAccessService moderationAccessService,
            ClassTestGradingAssignmentService classTestGradingAssignmentService) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
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
     * -> chuyển PENDING_REVIEW cho giáo viên chấm tay, KHÔNG gọi AI (xem
     * {@link #moveToPendingReviewWithoutScores}).
     */
    private void recomputeInvalidatedSessions(java.util.UUID candidateId) {
        for (var session : examSessionRepository.findByCandidateId(candidateId)) {
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
                moveToPendingReviewWithoutScores(result, session.getId());
            }
        }
    }

    /**
     * Bài CHƯA từng được chấm câu nào: chuyển thẳng sang {@code PENDING_REVIEW} cho giáo viên
     * chấm tay, KHÔNG gọi AI.
     *
     * <p><strong>Vì sao bỏ lời gọi AI (2026-08-16):</strong> bản trước chạy
     * {@code retryGradingExamSession} ở nhánh này. Nhưng bài rơi vào đây là bài bị buộc kết
     * thúc giữa chừng — thí sinh mới nói được một hai câu, thường còn dở câu. Bắt AI chấm một
     * bài như vậy vừa tốn một lượt gọi, vừa cho ra điểm không có nghĩa, trong khi thứ giáo viên
     * cần để phán quyết là bản ghi lời nói chứ không phải điểm máy. Bản ghi đó vẫn còn nguyên
     * trong {@code exam_item_response_turns} và nay đã hiện được ở màn chấm
     * (JpaExamGradingQueryRepository#turnsForItem).
     *
     * <p><strong>Vì sao KHÔNG đi qua {@code UpsertExamCandidateResultUseCase}:</strong> nó gọi
     * {@code ExamSessionResultCalculator.calculate}, mà hàm đó ném {@code NotFoundException}
     * ngay khi một câu trả lời thiếu evaluation (dòng 82-85). Không có evaluation nào thì không
     * có gì để tính — đặt thẳng trạng thái là cách đúng, thay vì nới một hàm dùng chung cho mọi
     * đường tính điểm chỉ để phục vụ ca này.
     *
     * <p>Điểm giữ nguyên (chưa có điểm nào để mà đổi); giáo viên chấm xong thì chính lần chấm
     * đó sinh evaluation, và từ đó mọi đường tính điểm thông thường chạy lại bình thường.
     */
    private void moveToPendingReviewWithoutScores(
            ExamCandidateResult result, UUID sessionId) {
        var now = Instant.now();
        result.setStatus(ExamCandidateResultStatus.PENDING_REVIEW);
        result.setUpdatedAt(now);
        result.setUpdatedBy(moderationAccessService.getCurrentUserId());
        examCandidateResultRepository.save(result);
        LOGGER.info(
            "Gỡ chặn phiên {}: chưa có evaluation nào, chuyển PENDING_REVIEW cho giáo viên chấm tay (không gọi AI).",
            sessionId
        );
    }
}
