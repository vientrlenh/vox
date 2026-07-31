package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.response.input.examgrading.SubmitGradingResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Giáo viên nộp điểm chấm tay — nộp là chốt, không có bước admin duyệt.
 *
 * <p>Giáo viên chấm theo <em>từng tiêu chí của từng phần</em>, không nhập điểm
 * tổng: mỗi phần nhận một bản HUMAN/FINALIZED, mọi bản cũ của các phần đó chuyển
 * SUPERSEDED, rồi {@link UpsertExamCandidateResultUseCase} tính lại tổng và xếp
 * loại từ toàn bộ item. Tổng vì thế luôn là hàm của các item.
 *
 * <p>Trạng thái kết quả KHÔNG bị cưỡng bức: gọi overload một tham số để
 * {@code resolveDefaultStatus} tự quyết. Chấm đủ cả bài và đã gỡ cờ thì nó tự trả
 * RELEASED; chấm dở thì còn item {@code requiresHumanReview} nên nó giữ
 * PENDING_REVIEW — an toàn tự nhiên, không cần điều kiện riêng ở đây.
 */
@Service
public class SubmitGradingUseCase implements IUseCase<SubmitGradingCommand, SubmitGradingResponse> {

    /**
     * exam_item_evaluations.graded_by_model là NOT NULL và dành cho tên model AI;
     * người chấm không có model nên tên engine đứng thay.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private final ExamGradingAccessService examGradingAccessService;
    private final GradingItemScoreResolver gradingItemScoreResolver;

    public SubmitGradingUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamSessionRepository examSessionRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase,
            ExamGradingAccessService examGradingAccessService,
            GradingItemScoreResolver gradingItemScoreResolver) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examSessionRepository = examSessionRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
        this.examGradingAccessService = examGradingAccessService;
        this.gradingItemScoreResolver = gradingItemScoreResolver;
    }

    @Override
    @Transactional
    public SubmitGradingResponse execute(SubmitGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.loadForGrading(command.assignmentId(), command.candidateResultId());
        examGradingAccessService.authorizeGrader(context, currentUserId);

        // assignment == null: nhà trường chấm trực tiếp một bài chưa có phân công --
        // không có gì để chốt COMPLETED, bỏ qua toàn bộ bookkeeping phân công bên dưới.
        var assignment = context.assignment();
        if (assignment != null && assignment.isCompleted()) {
            throw new IllegalStateException("Bạn đã nộp điểm cho bài thi này.");
        }
        // Chặn chấm lại bài đã công bố (QĐ #3): chỉ bài đang chờ chấm mới nhận điểm.
        if (context.candidateResult().getStatus() != ExamCandidateResultStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Bài thi này không còn ở trạng thái chờ chấm.");
        }

        // Nộp là chốt COMPLETED, nên bắt buộc phủ đủ mọi phần — nộp thiếu rồi chốt
        // sẽ khóa cứng phần chưa chấm (bài COMPLETED không gỡ được).
        var resolvedItems = gradingItemScoreResolver.resolve(context, command, true);
        var now = Instant.now();

        // Gỡ cờ nghi vấn (Mô hình A): giáo viên đã xem và kết luận không vi phạm.
        // Giữ nguyên flagReason để còn tra lại vì sao bài từng bị đánh dấu.
        var session = context.session();
        if (session.isFlagged()) {
            session.setFlagged(false);
            examSessionRepository.save(session);
        }

        // Bản AI và mọi bản chấm cũ của các phần được chấm đều lùi về SUPERSEDED,
        // để mỗi phần chỉ còn đúng một bản FINALIZED là nguồn điểm. Calculator dựa
        // vào việc AI bị flip trạng thái, KHÔNG ưu tiên HUMAN theo engineType.
        var responseIds = resolvedItems.stream()
            .map(item -> item.responseId()).toList();
        for (var evaluation : examItemEvaluationRepository.findByResponseIdIn(responseIds)) {
            if (evaluation.getStatus() != ExamItemEvaluationStatus.SUPERSEDED) {
                evaluation.setStatus(ExamItemEvaluationStatus.SUPERSEDED);
                examItemEvaluationRepository.save(evaluation);
            }
        }

        var criterionScores = new ArrayList<ExamItemCriterionScore>();
        for (var item : resolvedItems) {
            var savedEvaluation = examItemEvaluationRepository.save(new ExamItemEvaluation(
                item.responseId(),
                item.paperItemId(),
                ExamEvaluationEngineType.HUMAN,
                HUMAN_GRADER,
                null,
                currentUserId,
                item.itemScore(),
                item.itemScore(),
                null,
                // false là điều kiện để auto-resolve nhả bài về RELEASED: người đã
                // chấm thì không còn gì để chờ người chấm nữa.
                false,
                null,
                false,
                false,
                null,
                null,
                item.feedbackSummary(),
                null,
                null,
                ExamItemEvaluationStatus.FINALIZED,
                now
            ));
            item.criterionScores().forEach(score -> criterionScores.add(new ExamItemCriterionScore(
                savedEvaluation.getId(),
                score.rubricCriterionId(),
                score.score(),
                score.score(),
                score.rationale()
            )));
        }
        examItemCriterionScoreRepository.saveAll(criterionScores);

        // Một lần gọi là đủ: calculator quét toàn bộ item nên thấy hết bản mới.
        var recalculated = upsertExamCandidateResultUseCase.execute(context.candidateResult().getSessionId());

        if (assignment != null) {
            assignment.setStatus(GradingAssignmentStatus.COMPLETED);
            assignment.setCompletedAt(now);
            examGradingAssignmentRepository.save(assignment);
        }

        return new SubmitGradingResponse(
            context.candidateResult().getId(),
            recalculated.getTotalScore(),
            recalculated.getStatus() == null ? null : recalculated.getStatus().name()
        );
    }
}
