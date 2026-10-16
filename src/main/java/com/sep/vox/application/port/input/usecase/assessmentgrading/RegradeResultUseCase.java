package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingOutcome;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.repository.assessment.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.domain.service.assessment.GradingRoundPolicy;

/**
 * Giáo viên chấm lại — nộp là chốt, không có bước admin duyệt.
 *
 * <p>Chấm theo <em>từng tiêu chí của từng phần</em>, không nhập điểm tổng: mỗi phần
 * nhận một bản HUMAN/FINALIZED, mọi bản cũ của các phần đó chuyển SUPERSEDED, rồi
 * {@link UpsertAssessmentCandidateResultUseCase} tính lại tổng và xếp loại từ toàn bộ item.
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
     * assessment_item_evaluations.graded_by_model là NOT NULL và dành cho tên model AI;
     * người chấm không có model nên tên engine đứng thay. Người chấm thật nằm ở
     * {@code reviewerId}.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final GradingActionSupport gradingActionSupport;
    private final GradingItemScoreResolver gradingItemScoreResolver;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;

    public RegradeResultUseCase(
            GradingActionSupport gradingActionSupport,
            GradingItemScoreResolver gradingItemScoreResolver,
            AssessmentItemEvaluationRepository assessmentItemEvaluationRepository,
            AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase) {
        this.gradingActionSupport = gradingActionSupport;
        this.gradingItemScoreResolver = gradingItemScoreResolver;
        this.assessmentItemEvaluationRepository = assessmentItemEvaluationRepository;
        this.assessmentItemCriterionScoreRepository = assessmentItemCriterionScoreRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.upsertAssessmentCandidateResultUseCase = upsertAssessmentCandidateResultUseCase;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(SubmitGradingCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), AssessmentGradingOutcome.REGRADED, null);
        var context = prepared.context();

        // Nộp là chốt COMPLETED, nên bắt buộc phủ đủ mọi phần — nộp thiếu rồi chốt
        // sẽ khóa cứng phần chưa chấm (bài COMPLETED không gỡ được).
        var resolvedItems = gradingItemScoreResolver.resolve(context, command, true);
        var now = Instant.now();

        // Gỡ cờ nghi vấn: giáo viên đã xem và kết luận không vi phạm.
        var session = context.session();
        if (session.isFlagged()) {
            session.setFlagged(false);
            assessmentSessionRepository.save(session);
        }

        // Bản AI và mọi bản chấm cũ của các phần được chấm đều lùi về SUPERSEDED,
        // để mỗi phần chỉ còn đúng một bản FINALIZED là nguồn điểm. Calculator dựa
        // vào việc bản cũ bị flip trạng thái, KHÔNG ưu tiên HUMAN theo engineType.
        var responseIds = resolvedItems.stream()
            .map(item -> item.responseId()).toList();
        for (var evaluation : assessmentItemEvaluationRepository.findByResponseIdIn(responseIds)) {
            if (evaluation.getStatus() != AssessmentItemEvaluationStatus.SUPERSEDED) {
                evaluation.setStatus(AssessmentItemEvaluationStatus.SUPERSEDED);
                assessmentItemEvaluationRepository.save(evaluation);
            }
        }

        var criterionScores = new ArrayList<AssessmentItemCriterionScore>();
        for (var item : resolvedItems) {
            var savedEvaluation = assessmentItemEvaluationRepository.save(new AssessmentItemEvaluation(
                item.responseId(),
                item.paperItemId(),
                AssessmentEvaluationEngineType.HUMAN,
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
                AssessmentItemEvaluationStatus.FINALIZED,
                now
            ));
            item.criterionScores().forEach(score -> criterionScores.add(new AssessmentItemCriterionScore(
                savedEvaluation.getId(),
                score.rubricCriterionId(),
                score.score(),
                score.score(),
                score.rationale()
            )));
        }
        assessmentItemCriterionScoreRepository.saveAll(criterionScores);

        // Một lần gọi là đủ: calculator quét toàn bộ item nên thấy hết bản mới.
        // Trạng thái truyền tường minh — xem javadoc lớp.
        var targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), AssessmentGradingOutcome.REGRADED);
        var recalculated = upsertAssessmentCandidateResultUseCase.execute(
            context.candidateResult().getSessionId(),
            targetStatus == null ? context.candidateResult().getStatus() : targetStatus);

        // KHÔNG còn phát HumanGradingSubmittedEvent để suy nhãn điểm yếu từ feedbackSummary
        // giáo viên. Cả người nghe (GradingDiagnosticsInferenceJob) lẫn đích đến
        // (weakness_observation) đã bị gỡ; giữ lại thì mỗi lần giáo viên nộp điểm vẫn phải
        // dựng danh sách item và gọi một lượt LLM cho một cái hộp không đáy.

        gradingActionSupport.finish(prepared, recalculated);

        return new GradingActionResponse(
            command.assignmentId(),
            recalculated.getId(),
            AssessmentGradingOutcome.REGRADED.name(),
            recalculated.getStatus() == null ? null : recalculated.getStatus().name(),
            recalculated.getTotalScore(),
            null
        );
    }

    /** Vòng nào cho phép chấm lại — dùng ở tài liệu và test, giữ cạnh luật cho dễ tra. */
    static boolean supports(AssessmentGradingRoundType roundType) {
        return GradingRoundPolicy.isAllowed(roundType, AssessmentGradingOutcome.REGRADED);
    }
}
