package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * A teacher regrades -- submitting is final, there is no admin approval step.
 *
 * <p>Grades <em>each criterion of each part</em>, with no total score entered: each part
 * receives one HUMAN/FINALIZED version, every old version of those parts moves to SUPERSEDED, and then
 * {@link UpsertAssessmentCandidateResultUseCase} recomputes the total and the grade from all the items.
 * The total is therefore always a function of the items, in all four rounds.
 *
 * <p>The target status comes from {@code GradingRoundPolicy}, NOT left to
 * {@code resolveDefaultStatus}: in the post-check round the attempt must stay RELEASED,
 * while the default would pull it back to PENDING_REVIEW if any item still needs a look -- i.e. taking back
 * the student's already published score for a technical reason.
 */
@Service
@RequiredArgsConstructor
public class RegradeResultUseCase implements IUseCase<SubmitGradingCommand, GradingActionResponse> {

    /**
     * assessment_item_evaluations.graded_by_model is NOT NULL and is meant for the name of an AI model;
     * a human grader has no model so the engine name stands in. The real grader is in
     * {@code reviewerId}.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final GradingActionSupport gradingActionSupport;
    private final GradingItemScoreResolver gradingItemScoreResolver;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;

    @Override
    @Transactional
    public GradingActionResponse execute(SubmitGradingCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.REGRADED, null);
        AssessmentGradingAccessService.GradingContext context = prepared.context();

        // Nộp là chốt COMPLETED, nên bắt buộc phủ đủ mọi phần — nộp thiếu rồi chốt
        // sẽ khóa cứng phần chưa chấm (bài COMPLETED không gỡ được).
        List<GradingItemScoreResolver.ResolvedItem> resolvedItems = gradingItemScoreResolver.resolve(context, command, true);
        Instant now = Instant.now();

        // Gỡ cờ nghi vấn: giáo viên đã xem và kết luận không vi phạm.
        AssessmentSession session = context.session();
        if (session.isFlagged()) {
            session.setFlagged(false);
            assessmentSessionRepository.save(session);
        }

        // Bản AI và mọi bản chấm cũ của các phần được chấm đều lùi về SUPERSEDED,
        // để mỗi phần chỉ còn đúng một bản FINALIZED là nguồn điểm. Calculator dựa
        // vào việc bản cũ bị flip trạng thái, KHÔNG ưu tiên HUMAN theo engineType.
        List<UUID> responseIds = resolvedItems.stream()
            .map(item -> item.responseId()).toList();
        for (AssessmentItemEvaluation evaluation : assessmentItemEvaluationRepository.findByResponseIdIn(responseIds)) {
            if (evaluation.getStatus() != AssessmentItemEvaluationStatus.SUPERSEDED) {
                evaluation.setStatus(AssessmentItemEvaluationStatus.SUPERSEDED);
                assessmentItemEvaluationRepository.save(evaluation);
            }
        }

        List<AssessmentItemCriterionScore> criterionScores = new ArrayList<AssessmentItemCriterionScore>();
        for (GradingItemScoreResolver.ResolvedItem item : resolvedItems) {
            AssessmentItemEvaluation savedEvaluation = assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.humanGraded(
                    item.responseId(),
                    item.paperItemId(),
                    HUMAN_GRADER,
                    prepared.currentUserId(),
                    item.itemScore(),
                    item.feedbackSummary(),
                    now));
            item.criterionScores().forEach(score -> criterionScores.add(AssessmentItemCriterionScore.scoredWithoutBand(
                    savedEvaluation.getId(),
                    score.rubricCriterionId(),
                    score.score(),
                    score.rationale())));
        }
        assessmentItemCriterionScoreRepository.saveAll(criterionScores);

        // Một lần gọi là đủ: calculator quét toàn bộ item nên thấy hết bản mới.
        // Trạng thái truyền tường minh — xem javadoc lớp.
        AssessmentCandidateResultStatus targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), GradingOutcome.REGRADED);
        AssessmentCandidateResult recalculated = upsertAssessmentCandidateResultUseCase.execute(
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
