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
 * Validate + quy đổi điểm tiêu chí thành điểm phần thi.
 *
 * <p>Tách riêng vì {@code RegradeResultUseCase} và {@code PreviewGradingUseCase}
 * BẮT BUỘC đi qua đúng một đường: nếu preview và submit tính khác nhau dù chỉ ở
 * chỗ làm tròn, con số giáo viên thấy trước khi bấm Nộp sẽ không phải con số học
 * sinh nhận — đó chính là lý do endpoint preview tồn tại.
 */
@Service
@RequiredArgsConstructor
public class GradingItemScoreResolver {

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;

    /** Một phần thi đã chấm xong, sẵn sàng ghi xuống hoặc đem đi tính thử. */
    public record ResolvedItem(
        UUID paperItemId,
        UUID responseId,
        BigDecimal itemScore,
        String feedbackSummary,
        List<SubmitGradingCommand.CriterionScoreItem> criterionScores,
        // RubricCriterion tuong ung criterionScores -- dung de bao HumanGradingSubmittedEvent
        // (suy nhan diem yeu tu feedbackSummary), khong tham gia tinh diem.
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
            throw new IllegalArgumentException("Phải chấm điểm cho ít nhất một phần thi.");
        }
        List<UUID> paperItemIds = requireDistinctPaperItemIds(items);

        Map<UUID, AssessmentItemResponse> responsesByPaperItemId = loadResponsesByPaperItemId(context);
        if (enforceFullCoverage && !new HashSet<>(paperItemIds).containsAll(responsesByPaperItemId.keySet())) {
            throw new IllegalArgumentException(
                "Phải chấm đủ tất cả " + responsesByPaperItemId.size() + " phần thi của bài.");
        }
        Map<UUID, RubricCriterion> criteria = loadCriteriaById(context);
        RubricVersion rubricVersion = rubricVersionRepository.findById(context.candidateResult().getRubricVersionId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rubric version của bài thi."));

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
            throw new IllegalArgumentException("Thiếu phần thi cần chấm.");
        }
        if (new HashSet<>(paperItemIds).size() != paperItemIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng phần thi.");
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
            throw new IllegalArgumentException("Phần thi không thuộc bài thi này.");
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
            throw new IllegalArgumentException("Phải chấm điểm cho các tiêu chí.");
        }
        List<UUID> submittedIds = scores.stream()
            .map(item -> item.rubricCriterionId()).toList();
        if (submittedIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Thiếu tiêu chí chấm điểm.");
        }
        if (new HashSet<>(submittedIds).size() != submittedIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng tiêu chí.");
        }
        List<String> missingRequired = criteria.values().stream()
            .filter(criterion -> criterion.isRequired())
            .filter(criterion -> !submittedIds.contains(criterion.getId()))
            .map(criterion -> criterion.getName())
            .toList();
        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException(
                "Phải chấm đủ các tiêu chí bắt buộc: " + String.join(", ", missingRequired) + ".");
        }
    }

    private RubricItemScoreFormula.ScoredCriterion scoredCriterion(
            SubmitGradingCommand.CriterionScoreItem item, Map<UUID, RubricCriterion> criteria) {
        RubricCriterion criterion = criteria.get(item.rubricCriterionId());
        if (criterion == null) {
            throw new IllegalArgumentException("Tiêu chí không thuộc rubric của bài thi này.");
        }
        if (item.score() == null) {
            throw new IllegalArgumentException("Phải chấm điểm cho tiêu chí " + criterion.getName() + ".");
        }
        if (item.score().compareTo(criterion.getMinScore()) < 0
                || item.score().compareTo(criterion.getMaxScore()) > 0) {
            throw new IllegalArgumentException("Điểm tiêu chí " + criterion.getName()
                + " phải nằm trong khoảng " + criterion.getMinScore() + " - " + criterion.getMaxScore() + ".");
        }
        return new RubricItemScoreFormula.ScoredCriterion(item.score(), criterion.getWeight());
    }
}
