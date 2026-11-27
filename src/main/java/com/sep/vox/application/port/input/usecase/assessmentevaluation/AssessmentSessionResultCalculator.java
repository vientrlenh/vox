package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.assessment.Assessment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentSessionResultCalculator {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;

    public CalculatedAssessmentSessionResult calculate(UUID sessionId) {
        LoadedSession loaded = load(sessionId);
        Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId = evaluationsByResponseId(loaded.responses());
        Map<UUID, BigDecimal> itemScoreByResponseId = itemScoresByResponseId(
            loaded.responses(), evaluationsByResponseId, Map.<UUID, BigDecimal>of());
        RolledUpScores rolled = rollUp(loaded, itemScoreByResponseId);
        boolean anyRequiresHumanReview = evaluationsByResponseId.values().stream()
            .anyMatch(evaluation -> evaluation.isRequiresHumanReview());
        return toCalculatedResult(loaded, rolled, anyRequiresHumanReview);
    }

    private CalculatedAssessmentSessionResult toCalculatedResult(
            LoadedSession loaded, RolledUpScores rolled, boolean anyRequiresHumanReview) {
        FrameworkResultBand targetFrameworkBand = loaded.policy().getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(loaded.policy().getTargetFrameworkBandId()).orElse(null);
        return new CalculatedAssessmentSessionResult(
            loaded.session().getId(),
            loaded.assessmentId(),
            loaded.session().getPaperId(),
            loaded.session().getCandidateId(),
            loaded.policy(),
            rolled.totalScore(),
            targetFrameworkBand,
            resolveRubricResultBand(loaded.policy(), rolled.totalScore()),
            rolled.sections(),
            rolled.items(),
            anyRequiresHumanReview
        );
    }

    /**
     * Total score "if this score set were submitted" -- the same formula as {@link #calculate},
     * but it writes NOTHING. The manual grading screen needs it because the total is weighted per
     * item and then per section (not an arithmetic mean), so the frontend cannot compute a
     * matching one itself.
     *
     * <p>{@code overrides} are the scores the teacher is currently typing, keyed by responseId. A
     * response with no override takes the score of the evaluation currently in effect -- so a
     * half-finished grading can still be previewed and the parts not yet entered keep the AI score.
     */
    public PreviewedAssessmentSessionResult preview(UUID sessionId, Map<UUID, BigDecimal> overrides) {
        LoadedSession loaded = load(sessionId);
        Map<UUID, BigDecimal> safeOverrides = overrides == null ? Map.<UUID, BigDecimal>of() : overrides;
        Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId = evaluationsByResponseId(loaded.responses());
        Map<UUID, BigDecimal> itemScoreByResponseId = itemScoresByResponseId(
            loaded.responses(), evaluationsByResponseId, safeOverrides);

        RolledUpScores rolled = rollUp(loaded, itemScoreByResponseId);
        RubricResultBand band = resolveRubricResultBand(loaded.policy(), rolled.totalScore());
        return new PreviewedAssessmentSessionResult(
            rolled.totalScore(),
            band == null ? null : band.getName(),
            rolled.sections(),
            rolled.items()
        );
    }

    private Map<UUID, BigDecimal> itemScoresByResponseId(
            List<AssessmentItemResponse> responses,
            Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId,
            Map<UUID, BigDecimal> overrides) {
        Map<UUID, BigDecimal> itemScoreByResponseId = new LinkedHashMap<UUID, BigDecimal>();
        for (AssessmentItemResponse response : responses) {
            BigDecimal override = overrides.get(response.getId());
            itemScoreByResponseId.put(response.getId(),
                override != null ? override : evaluatedItemScore(response, evaluationsByResponseId));
        }
        return itemScoreByResponseId;
    }

    private BigDecimal evaluatedItemScore(
            AssessmentItemResponse response, Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId) {
        AssessmentItemEvaluation evaluation = evaluationsByResponseId.get(response.getId());
        if (evaluation == null) {
            throw new ResourceNotFoundException("No evaluation found for answer " + response.getId());
        }
        return evaluation.getItemScore() == null ? BigDecimal.ZERO : evaluation.getItemScore();
    }

    /**
     * The pure calculation part: item -> section -> total. It does not touch any repository, so
     * {@code calculate} and {@code preview} share exactly one formula.
     */
    private RolledUpScores rollUp(LoadedSession loaded, Map<UUID, BigDecimal> itemScoreByResponseId) {
        SectionTotals totals = newSectionTotals(loaded.orderedSections());
        List<ItemScore> itemScores = new ArrayList<ItemScore>();
        for (AssessmentItemResponse response : loaded.responses()) {
            itemScores.add(addItemToSectionTotals(loaded, totals, response, itemScoreByResponseId));
        }
        normalizeSectionScores(totals, loaded.scaleFloor());
        return new RolledUpScores(
            calculateTotalScore(loaded.orderedSections(), totals.sectionScores(), loaded.scaleFloor()),
            sectionScoresInOrder(loaded, totals.sectionScores()),
            itemScores);
    }

    private SectionTotals newSectionTotals(List<AssessmentPaperSection> orderedSections) {
        SectionTotals totals = new SectionTotals(
            new LinkedHashMap<UUID, BigDecimal>(),
            new LinkedHashMap<UUID, BigDecimal>(),
            new LinkedHashMap<UUID, BigDecimal>(),
            new LinkedHashMap<UUID, Integer>());
        orderedSections.forEach(section -> {
            totals.sectionScores().put(section.getId(), scaled(BigDecimal.ZERO));
            totals.sectionWeightSums().put(section.getId(), BigDecimal.ZERO);
            totals.sectionPlainSums().put(section.getId(), BigDecimal.ZERO);
            totals.sectionItemCounts().put(section.getId(), 0);
        });
        return totals;
    }

    private ItemScore addItemToSectionTotals(
            LoadedSession loaded,
            SectionTotals totals,
            AssessmentItemResponse response,
            Map<UUID, BigDecimal> itemScoreByResponseId) {
        AssessmentPaperItem paperItem = loaded.paperItemsById().get(response.getPaperItemId());
        if (paperItem == null) {
            throw new ResourceNotFoundException("Paper item not found: " + response.getPaperItemId());
        }
        BigDecimal rawScore = itemScoreByResponseId.getOrDefault(response.getId(), BigDecimal.ZERO);
        BigDecimal itemScore = scaled(rawScore == null ? BigDecimal.ZERO : rawScore);
        BigDecimal weight = paperItem.getWeight() == null ? BigDecimal.ZERO : paperItem.getWeight();
        BigDecimal weightedScore = scaled(itemScore.multiply(weight));
        addToSectionTotals(totals, paperItem.getSectionId(), itemScore, weight, weightedScore);
        return new ItemScore(
            paperItem.getId(),
            response.getId(),
            paperItem.getSectionId(),
            paperItem.getQuestionId(),
            itemScore,
            weightedScore
        );
    }

    private void addToSectionTotals(
            SectionTotals totals, UUID sectionId, BigDecimal itemScore, BigDecimal weight, BigDecimal weightedScore) {
        totals.sectionScores().compute(sectionId, (ignored, current) ->
            scaled((current == null ? BigDecimal.ZERO : current).add(weightedScore))
        );
        totals.sectionWeightSums().compute(sectionId, (ignored, current) ->
            (current == null ? BigDecimal.ZERO : current).add(weight)
        );
        totals.sectionPlainSums().compute(sectionId, (ignored, current) ->
            (current == null ? BigDecimal.ZERO : current).add(itemScore)
        );
        totals.sectionItemCounts().merge(sectionId, 1, (a, b) -> Integer.sum(a, b));
    }

    /**
     * sectionScores is reused in calculateTotalScore, so it is normalized IN PLACE before the
     * display list is built -- otherwise the section score the user sees and the one that enters
     * the total would be two different numbers.
     */
    private void normalizeSectionScores(SectionTotals totals, BigDecimal scaleFloor) {
        totals.sectionScores().replaceAll((sectionId, weightedSum) -> normalizedSectionScore(
            weightedSum,
            totals.sectionWeightSums().getOrDefault(sectionId, BigDecimal.ZERO),
            totals.sectionPlainSums().getOrDefault(sectionId, BigDecimal.ZERO),
            totals.sectionItemCounts().getOrDefault(sectionId, 0),
            scaleFloor
        ));
    }

    private List<SectionScore> sectionScoresInOrder(LoadedSession loaded, Map<UUID, BigDecimal> sectionScores) {
        return loaded.orderedSections().stream()
            .map(section -> new SectionScore(
                section.getId(),
                section.getTitle(),
                sectionScores.getOrDefault(section.getId(), loaded.scaleFloor())
            ))
            .toList();
    }

    /** Everything one scoring run needs, loaded exactly once for both calculate and preview. */
    private LoadedSession load(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        AssessmentPolicy policy = loadPolicyOf(assessment);
        List<AssessmentPaperSection> orderedSections = orderedSectionsOfPaper(session.getPaperId());
        Map<UUID, AssessmentPaperItem> paperItemsById = paperItemsById(session.getPaperId());
        List<AssessmentItemResponse> responses = responsesInDisplayOrder(sessionId, orderedSections, paperItemsById);

        return new LoadedSession(
            session, assessment.getId(), policy, orderedSections, responses, paperItemsById,
            resolveScaleFloor(policy));
    }

    private AssessmentPolicy loadPolicyOf(Assessment assessment) {
        if (assessment.getAssessmentPolicyId() == null) {
            throw new ResourceNotFoundException("The assessment has no assessment policy assigned");
        }
        return assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment policy not found"));
    }

    private List<AssessmentPaperSection> orderedSectionsOfPaper(UUID paperId) {
        return assessmentPaperSectionRepository.findByPaperId(paperId).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
    }

    private Map<UUID, AssessmentPaperItem> paperItemsById(UUID paperId) {
        return assessmentPaperItemRepository.findByPaperId(paperId).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity(), (left, right) -> left));
    }

    /**
     * Sorted RIGHT HERE rather than where the display list is built: all three loops in this class
     * (calculate, preview, rollUp) iterate `responses`, so sorting at the source gives every path
     * the same order.
     *
     * <p>findBySessionId has no ORDER BY, so the order is whatever Postgres returns. Observed for
     * real (session 019fff59, 2026-08-14): it returned Part 2's items before Part 1's, and not by
     * submission time either (Part 1 submitted at 08:19, Part 2 at 08:21). The client numbers
     * "Question {index + 1}" by array position, so the student saw Question 1 carrying Part 2's
     * score.
     *
     * <p>The SECTION must be compared FIRST and the item second: in a two-part paper with one
     * question per part both questions have order = 1, so comparing only items ties and the order
     * becomes arbitrary again. Ties are broken by id so two consecutive calls do not give two
     * different orders when corrupt data ties both keys.
     */
    private List<AssessmentItemResponse> responsesInDisplayOrder(
            UUID sessionId,
            List<AssessmentPaperSection> orderedSections,
            Map<UUID, AssessmentPaperItem> paperItemsById) {
        Map<UUID, Integer> sectionRankById = new HashMap<UUID, Integer>();
        for (int index = 0; index < orderedSections.size(); index++) {
            sectionRankById.put(orderedSections.get(index).getId(), index);
        }
        return assessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .sorted(Comparator
                .<AssessmentItemResponse>comparingInt(response -> {
                    AssessmentPaperItem paperItem = paperItemsById.get(response.getPaperItemId());
                    return paperItem == null
                        ? Integer.MAX_VALUE
                        : sectionRankById.getOrDefault(paperItem.getSectionId(), Integer.MAX_VALUE);
                })
                .thenComparingInt(response -> {
                    AssessmentPaperItem paperItem = paperItemsById.get(response.getPaperItemId());
                    return paperItem == null ? Integer.MAX_VALUE : paperItem.getOrder();
                })
                .thenComparing(response -> response.getId()))
            .toList();
    }

    /**
     * Batched (not one findLatestByResponseId/findById per response, which was
     * a real N+1 -- this runs on every GET of an assessment result, not just once at
     * grading time).
     */
    private Map<UUID, AssessmentItemEvaluation> evaluationsByResponseId(List<AssessmentItemResponse> responses) {
        List<UUID> responseIds = responses.stream().map(response -> response.getId()).toList();
        return assessmentItemEvaluationRepository.findLatestByResponseIdIn(responseIds).stream()
            .collect(Collectors.toMap(evaluation -> evaluation.getResponseId(), Function.identity(), (left, right) -> left));
    }

    private RubricResultBand resolveRubricResultBand(AssessmentPolicy policy, BigDecimal totalScore) {
        if (policy.getRubricVersionId() == null) {
            return null;
        }
        List<RubricResultBand> matchingBands = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(band -> band.getOrder()))
            .filter(band -> within(totalScore, band.getScoreMin(), band.getScoreMax()))
            .toList();
        if (matchingBands.isEmpty()) {
            log.info(
                "No result band matches the total score {} (rubricVersionId={}) -- saving the result without a classification.",
                totalScore, policy.getRubricVersionId()
            );
            return null;
        }
        if (matchingBands.size() > 1) {
            log.error(
                "Result band configuration OVERLAPS: the total score {} matches {} bands of rubricVersionId={}."
                    + " Using the band with the smallest order ({}). An admin needs to fix the bands.",
                totalScore, matchingBands.size(), policy.getRubricVersionId(),
                matchingBands.get(0).getCode()
            );
        }
        return matchingBands.get(0);
    }

    private boolean within(BigDecimal score, BigDecimal min, BigDecimal max) {
        if (score == null || min == null || max == null) {
            return false;
        }
        return score.compareTo(min) >= 0 && score.compareTo(max) <= 0;
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * The floor of the rubric scale used by this policy -- the score of a section/submission that has nothing to compute.
     *
     * <p>Before 2026-08-11 those places returned a plain 0. 0 is only right when the scale starts at 0; on a
     * 4-10 scale it sits below the floor and no classification band can contain it.
     *
     * <p>If the rubric version cannot be resolved, fall back to 0, keeping the old behavior for data with missing configuration.
     */
    private BigDecimal resolveScaleFloor(AssessmentPolicy policy) {
        if (policy.getRubricVersionId() == null) {
            return scaled(BigDecimal.ZERO);
        }
        return rubricVersionRepository.findById(policy.getRubricVersionId())
            .map(version -> version.getScoringScaleMin())
            .map(this::scaled)
            .orElseGet(() -> scaled(BigDecimal.ZERO));
    }

    /**
     * Normalizes the section score: {@code Σ(questionScore × questionWeight) / Σ questionWeight}.
     *
     * <p>This division was ADDED on 2026-08-11. Before that, the question→section level only summed the products
     * without dividing, while the section→total level right below it did divide -- two opposite conventions in the
     * same function. It was harmless in exactly one case: the question weights in a section add up to exactly 1.
     * Any deviation makes the section score leave the rubric scale, and when the total leaves the scale NO
     * classification band matches -- the student has a score but no classification.
     *
     * <p>A measured example: a section with 2 questions, each of weight 1.00, scoring 83.47 and 74.78 on a
     * scale of 100. The old version gave a section score of 158.25; this version gives 79.13.
     *
     * <p>Two fallbacks when the division is impossible, both producing a score INSIDE the scale:
     *
     * <ul>
     *   <li>Every question in the section has weight 0 -- return the arithmetic mean of the questions. This reads the
     *       intent "if nobody declared weights, the questions are equal", not "every question is worthless": the
     *       previous version returned the weighted sum, i.e. 0, and on a 4-10 scale 0 is outside the scale.
     *   <li>A section with no question -- return the scale floor, for the same reason.
     * </ul>
     */
    private BigDecimal normalizedSectionScore(
            BigDecimal weightedSum,
            BigDecimal weightSum,
            BigDecimal plainSum,
            int itemCount,
            BigDecimal scaleFloor) {
        if (weightSum != null && weightSum.compareTo(BigDecimal.ZERO) > 0) {
            return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
        }
        if (itemCount > 0) {
            return plainSum.divide(BigDecimal.valueOf(itemCount), 2, RoundingMode.HALF_UP);
        }
        return scaleFloor;
    }

    private BigDecimal calculateTotalScore(
            List<AssessmentPaperSection> orderedSections,
            LinkedHashMap<UUID, BigDecimal> sectionScores,
            BigDecimal scaleFloor) {
        if (orderedSections.isEmpty()) {
            return scaleFloor;
        }

        BigDecimal totalWeight = orderedSections.stream()
            .map(section -> section.getWeight() == null ? BigDecimal.ZERO : section.getWeight())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weightedTotal = orderedSections.stream()
                .map(section -> sectionScores.getOrDefault(section.getId(), scaleFloor)
                    .multiply(section.getWeight() == null ? BigDecimal.ZERO : section.getWeight()))
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
            return scaled(weightedTotal.divide(totalWeight, 2, RoundingMode.HALF_UP));
        }

        BigDecimal plainTotal = sectionScores.values().stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        return scaled(plainTotal.divide(BigDecimal.valueOf(sectionScores.size()), 2, RoundingMode.HALF_UP));
    }

    private record LoadedSession(
        AssessmentSession session,
        UUID assessmentId,
        AssessmentPolicy policy,
        List<AssessmentPaperSection> orderedSections,
        List<AssessmentItemResponse> responses,
        Map<UUID, AssessmentPaperItem> paperItemsById,
        /** The rubric scale floor -- the score of a section/submission that has nothing to compute. */
        BigDecimal scaleFloor
    ) {
    }

    /**
     * Running totals per section. {@code sectionWeightSums} is the sum of item weights PER SECTION
     * -- the denominator of the normalization. {@code sectionPlainSums} and {@code sectionItemCounts}
     * are the sum of item scores WITHOUT weights and the item count -- used only when every weight
     * in the section is 0, which falls back to the arithmetic mean (see normalizedSectionScore).
     */
    private record SectionTotals(
        LinkedHashMap<UUID, BigDecimal> sectionScores,
        Map<UUID, BigDecimal> sectionWeightSums,
        Map<UUID, BigDecimal> sectionPlainSums,
        Map<UUID, Integer> sectionItemCounts
    ) {
    }

    private record RolledUpScores(
        BigDecimal totalScore,
        List<SectionScore> sections,
        List<ItemScore> items
    ) {
    }

    public record CalculatedAssessmentSessionResult(
        UUID sessionId,
        UUID assessmentId,
        UUID paperId,
        UUID candidateId,
        AssessmentPolicy policy,
        BigDecimal totalScore,
        FrameworkResultBand targetFrameworkBand,
        RubricResultBand rubricResultBand,
        List<SectionScore> sections,
        List<ItemScore> items,
        boolean anyRequiresHumanReview
    ) {
    }

    /** Result of a "trial grading": only what the grading screen needs, nothing to be written to the DB. */
    public record PreviewedAssessmentSessionResult(
        BigDecimal totalScore,
        String resultBandName,
        List<SectionScore> sections,
        List<ItemScore> items
    ) {
    }

    public record SectionScore(
        UUID sectionId,
        String title,
        BigDecimal score
    ) {
    }

    /**
     * @param questionId the question behind this item. This class does NOT load the question content -- scoring
     *                   does not need to read the question, and pulling the questions table in here would make every score
     *                   computation carry one more query that does not serve the computation. The display side
     *                   ({@code ViewAssessmentSessionResultUseCase}) loads the content in batches from this id.
     */
    public record ItemScore(
        UUID paperItemId,
        UUID responseId,
        UUID sectionId,
        UUID questionId,
        BigDecimal itemScore,
        BigDecimal weightedScore
    ) {
    }
}
