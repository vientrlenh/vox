package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.rubric.RubricVersion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.service.assessment.RubricItemScoreFormula;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Validates + converts criterion scores into assessment-item scores.
 *
 * <p>Split out because {@code RegradeResultUseCase} and {@code PreviewGradingUseCase}
 * MUST go through exactly one path: if preview and submit compute differently even only in
 * rounding, the number the teacher sees before clicking Submit would not be the number the
 * student receives — which is exactly why the preview endpoint exists.
 */
@Service
@RequiredArgsConstructor
public class GradingItemScoreResolver {

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;

    /** One graded assessment item, ready to be written down or taken for a trial computation. */
    public record ResolvedItem(
        UUID paperItemId,
        UUID responseId,
        BigDecimal itemScore,
        String feedbackSummary,
        List<SubmitGradingCommand.CriterionScoreItem> criterionScores,
        // The RubricCriterion corresponding to criterionScores -- used to report HumanGradingSubmittedEvent
        // (infer weak points from feedbackSummary), not part of the score computation.
        List<RubricCriterion> criteria
    ) {
    }

    /**
     * @param enforceFullCoverage when true (the real submit flow): every item of the assessment must be
     *        covered. Preview passes false because it deliberately grades partially to watch the total build up.
     *        Finalizing as COMPLETED is only safe once everything is covered -- submitting with items missing and then
     *        finalizing would lock the ungraded items for good (an assessment that is already COMPLETED cannot be reopened).
     */
    @Transactional(readOnly = true)
    public List<ResolvedItem> resolve(
            GradingContext context, SubmitGradingCommand command, boolean enforceFullCoverage) {
        List<SubmitGradingCommand.ItemGrade> items = command.items() == null ? List.<SubmitGradingCommand.ItemGrade>of() : command.items();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("At least one assessment item must be scored.");
        }
        List<UUID> paperItemIds = requireDistinctPaperItemIds(items);

        Map<UUID, AssessmentItemResponse> responsesByPaperItemId = loadResponsesByPaperItemId(context);
        if (enforceFullCoverage && !new HashSet<>(paperItemIds).containsAll(responsesByPaperItemId.keySet())) {
            throw new IllegalArgumentException(
                "All " + responsesByPaperItemId.size() + " items of the assessment must be scored.");
        }
        Map<UUID, RubricCriterion> criteria = loadCriteriaById(context);
        RubricVersion rubricVersion = rubricVersionRepository.findById(context.candidateResult().getRubricVersionId())
            .orElseThrow(() -> new IllegalArgumentException("Rubric version of the assessment not found."));

        // Grade everything before returning -- one failing item must not leave the earlier items
        // with a half-finished score.
        List<ResolvedItem> resolved = new ArrayList<ResolvedItem>();
        for (SubmitGradingCommand.ItemGrade item : items) {
            resolved.add(resolveItem(item, responsesByPaperItemId, criteria, rubricVersion));
        }
        return resolved;
    }

    private Map<UUID, AssessmentItemResponse> loadResponsesByPaperItemId(GradingContext context) {
        return assessmentItemResponseRepository
            .findBySessionId(context.candidateResult().getSessionId()).stream()
            .collect(Collectors.toMap(res -> res.getPaperItemId(), Function.identity(),
                (left, right) -> left));
    }

    private Map<UUID, RubricCriterion> loadCriteriaById(GradingContext context) {
        return rubricCriterionRepository
            .findByRubricVersionId(context.candidateResult().getRubricVersionId()).stream()
            .collect(Collectors.toMap(criterion -> criterion.getId(), Function.identity(), (left, right) -> left));
    }

    private List<UUID> requireDistinctPaperItemIds(List<SubmitGradingCommand.ItemGrade> items) {
        List<UUID> paperItemIds = items.stream().map(itemGrade -> itemGrade.paperItemId()).toList();
        if (paperItemIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("The item to be scored is missing.");
        }
        if (new HashSet<>(paperItemIds).size() != paperItemIds.size()) {
            throw new IllegalArgumentException("The same item must not be scored twice.");
        }
        return paperItemIds;
    }

    private ResolvedItem resolveItem(
            SubmitGradingCommand.ItemGrade item,
            Map<UUID, AssessmentItemResponse> responsesByPaperItemId,
            Map<UUID, RubricCriterion> criteria,
            RubricVersion rubricVersion) {
        AssessmentItemResponse response = responsesByPaperItemId.get(item.paperItemId());
        if (response == null) {
            throw new IllegalArgumentException("The item does not belong to this assessment.");
        }
        return new ResolvedItem(
            item.paperItemId(),
            response.getId(),
            itemScore(item.criterionScores(), criteria, rubricVersion.getTotalScoreMethod(),
                rubricVersion.getScoringScaleMin(), rubricVersion.getScoringScaleMax()),
            item.feedbackSummary(),
            item.criterionScores(),
            item.criterionScores().stream()
                .map(score -> criteria.get(score.rubricCriterionId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList()
        );
    }

    /**
     * Validates the score the teacher entered and then hands the computation to {@link RubricItemScoreFormula} -- a formula
     * SHARED with the AI grading path ({@code RecordAssessmentAttemptEvaluationUseCase}).
     *
     * <p>Before 2026-08-11 this was a second hand-copied version of the same formula, and the two had drifted apart:
     * the fallback branch (rubric without declared weights) here divided by the number of criteria SUBMITTED, the other one
     * divided by the number of criteria that MATCHED the rubric. Now both divide by the number of resolved criteria.
     *
     * <p>What stays specific here is the INPUT validation, which is deliberately stricter than the AI path: a person entering
     * scores by hand must be blocked right away on a missing mandatory criterion, a duplicate grade, or a score that falls outside the
     * criterion's range.
     */
    private BigDecimal itemScore(
            List<SubmitGradingCommand.CriterionScoreItem> scores,
            Map<UUID, RubricCriterion> criteria,
            RubricTotalScoreMethod totalScoreMethod,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax) {
        requireSubmittedCriteria(scores, criteria);
        List<RubricItemScoreFormula.ScoredCriterion> scored = new ArrayList<RubricItemScoreFormula.ScoredCriterion>();
        for (SubmitGradingCommand.CriterionScoreItem item : scores) {
            scored.add(scoredCriterion(item, criteria));
        }

        return RubricItemScoreFormula.compute(
            scored, scoringScaleMin, scoringScaleMax
        );
    }

    private void requireSubmittedCriteria(
            List<SubmitGradingCommand.CriterionScoreItem> scores, Map<UUID, RubricCriterion> criteria) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("The criteria must be scored.");
        }
        List<UUID> submittedIds = scores.stream()
            .map(item -> item.rubricCriterionId()).toList();
        if (submittedIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("The scoring criterion is missing.");
        }
        if (new HashSet<>(submittedIds).size() != submittedIds.size()) {
            throw new IllegalArgumentException("The same criterion must not be scored twice.");
        }
        List<String> missingRequired = criteria.values().stream()
            .filter(criterion -> criterion.isRequired())
            .filter(criterion -> !submittedIds.contains(criterion.getId()))
            .map(criterion -> criterion.getName())
            .toList();
        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException(
                "All required criteria must be scored: " + String.join(", ", missingRequired) + ".");
        }
    }

    private RubricItemScoreFormula.ScoredCriterion scoredCriterion(
            SubmitGradingCommand.CriterionScoreItem item, Map<UUID, RubricCriterion> criteria) {
        RubricCriterion criterion = criteria.get(item.rubricCriterionId());
        if (criterion == null) {
            throw new IllegalArgumentException("The criterion does not belong to the rubric of this assessment.");
        }
        if (item.score() == null) {
            throw new IllegalArgumentException("A score must be given for criterion " + criterion.getName() + ".");
        }
        if (item.score().compareTo(criterion.getMinScore()) < 0
                || item.score().compareTo(criterion.getMaxScore()) > 0) {
            throw new IllegalArgumentException("The score of criterion " + criterion.getName()
                + " must be within the range " + criterion.getMinScore() + " - " + criterion.getMaxScore() + ".");
        }
        return new RubricItemScoreFormula.ScoredCriterion(item.score(), criterion.getWeight());
    }
}
