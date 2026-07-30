package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundPolicy;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Giáo viên chấm lại — nộp là chốt, không có bước admin duyệt.
 *
 * <p>Chấm theo <em>từng tiêu chí của từng phần</em>, không nhập điểm tổng: mỗi phần
 * nhận một bản HUMAN/FINALIZED, mọi bản cũ của các phần đó chuyển SUPERSEDED, rồi
 * {@link UpsertExamCandidateResultUseCase} tính lại tổng và xếp loại từ toàn bộ item.
 * Tổng vì thế luôn là hàm của các item, ở cả bốn vòng.
 *
 * <p>Trạng thái đích lấy từ {@code GradingRoundPolicy}, KHÔNG để
 * {@code resolveDefaultStatus} tự quyết: ở vòng hậu kiểm bài phải giữ nguyên RELEASED,
 * mà mặc định sẽ kéo nó về PENDING_REVIEW nếu còn item cần soi — tức là thu hồi điểm
 * đã công bố của học sinh vì một lý do kỹ thuật.
 */
@Service
public class RegradeResultUseCase implements IUseCase<SubmitGradingCommand, GradingActionResponse> {

    /**
     * exam_item_evaluations.graded_by_model là NOT NULL và dành cho tên model AI;
     * người chấm không có model nên tên engine đứng thay. Người chấm thật nằm ở
     * {@code reviewerId}.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final GradingActionSupport gradingActionSupport;
    private final GradingItemScoreResolver gradingItemScoreResolver;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;

    public RegradeResultUseCase(
            GradingActionSupport gradingActionSupport,
            GradingItemScoreResolver gradingItemScoreResolver,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamSessionRepository examSessionRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase) {
        this.gradingActionSupport = gradingActionSupport;
        this.gradingItemScoreResolver = gradingItemScoreResolver;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examSessionRepository = examSessionRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(SubmitGradingCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.REGRADED, null);
        var context = prepared.context();

        // Nộp là chốt COMPLETED, nên bắt buộc phủ đủ mọi phần — nộp thiếu rồi chốt
        // sẽ khóa cứng phần chưa chấm (bài COMPLETED không gỡ được).
        var resolvedItems = gradingItemScoreResolver.resolve(context, command, true);
        var now = Instant.now();

        // Gỡ cờ nghi vấn: giáo viên đã xem và kết luận không vi phạm.
        var session = context.session();
        if (session.isFlagged()) {
            session.setFlagged(false);
            examSessionRepository.save(session);
        }

        // Bản AI và mọi bản chấm cũ của các phần được chấm đều lùi về SUPERSEDED,
        // để mỗi phần chỉ còn đúng một bản FINALIZED là nguồn điểm. Calculator dựa
        // vào việc bản cũ bị flip trạng thái, KHÔNG ưu tiên HUMAN theo engineType.
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
                prepared.currentUserId(),
                item.itemScore(),
                item.itemScore(),
                null,
                // false là điều kiện để calculator không giữ bài lại chờ người chấm:
                // người đã chấm thì không còn gì để chờ nữa.
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
        // Trạng thái truyền tường minh — xem javadoc lớp.
        var targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), GradingOutcome.REGRADED);
        var recalculated = upsertExamCandidateResultUseCase.execute(
            context.candidateResult().getSessionId(),
            targetStatus == null ? context.candidateResult().getStatus() : targetStatus);

        gradingActionSupport.finish(prepared, recalculated);

        return new GradingActionResponse(
            command.assignmentId(),
            recalculated.getId(),
            GradingOutcome.REGRADED.name(),
            recalculated.getStatus() == null ? null : recalculated.getStatus().name(),
            recalculated.getTotalScore(),
            null
        );
    }

    /** Vòng nào cho phép chấm lại — dùng ở tài liệu và test, giữ cạnh luật cho dễ tra. */
    static boolean supports(GradingRoundType roundType) {
        return GradingRoundPolicy.isAllowed(roundType, GradingOutcome.REGRADED);
    }
}
