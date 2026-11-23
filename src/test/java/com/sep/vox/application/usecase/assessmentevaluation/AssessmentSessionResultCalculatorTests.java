package com.sep.vox.application.usecase.assessmentevaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
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

/**
 * Khoá lại công thức tổng sau khi tách {@code rollUp}: {@code calculate} phải giữ
 * nguyên hành vi cho luồng AI và phúc khảo, còn {@code preview} phải cho ra đúng
 * con số mà {@code calculate} sẽ cho nếu bộ điểm kia được ghi thật.
 */
public class AssessmentSessionResultCalculatorTests {

    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentPolicyRepository assessmentPolicyRepository;
    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private RubricResultBandRepository rubricResultBandRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private RubricVersionRepository rubricVersionRepository;
    private AssessmentSessionResultCalculator calculator;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID speakingSectionId = UUID.randomUUID();
    private final UUID listeningSectionId = UUID.randomUUID();
    private final UUID speakingItemId = UUID.randomUUID();
    private final UUID listeningItemId = UUID.randomUUID();
    private final UUID speakingResponseId = UUID.randomUUID();
    private final UUID listeningResponseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        assessmentPaperSectionRepository = mock(AssessmentPaperSectionRepository.class);
        rubricResultBandRepository = mock(RubricResultBandRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        calculator = new AssessmentSessionResultCalculator(
            assessmentSessionRepository, assessmentRepository, assessmentPolicyRepository, assessmentItemResponseRepository,
            assessmentItemEvaluationRepository, assessmentPaperItemRepository, assessmentPaperSectionRepository,
            rubricResultBandRepository, frameworkResultBandRepository, rubricVersionRepository);

        // A 0-9 scale, matching the grade bands declared below. The scale floor is the score given
        // to a part that has no items -- see resolveScaleFloor.
        RubricVersion rubricVersion = mock(RubricVersion.class);
        when(rubricVersion.getScoringScaleMin()).thenReturn(new BigDecimal("0.00"));
        when(rubricVersionRepository.findById(rubricVersionId)).thenReturn(Optional.of(rubricVersion));

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setAssessmentId(assessmentId);
        session.setPaperId(paperId);
        session.setCandidateId(candidateId);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setAssessmentPolicyId(policyId);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        AssessmentPolicy policy = new AssessmentPolicy();
        policy.setId(policyId);
        policy.setRubricVersionId(rubricVersionId);
        when(assessmentPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        // Speaking carries more weight (0.60 / 0.40) -- if the formula slid back to a plain
        // average, these weights are what would expose it immediately.
        when(assessmentPaperSectionRepository.findByPaperId(paperId)).thenReturn(List.of(
            section(listeningSectionId, 2, "Listening", new BigDecimal("0.40")),
            section(speakingSectionId, 1, "Speaking", new BigDecimal("0.60"))));
        when(assessmentPaperItemRepository.findByPaperId(paperId)).thenReturn(List.of(
            AssessmentPaperItem.builder().id(speakingItemId).sectionId(speakingSectionId).paperId(paperId).order(1).weight(new BigDecimal("1.00")).build(),
            AssessmentPaperItem.builder().id(listeningItemId).sectionId(listeningSectionId).paperId(paperId).order(1).weight(new BigDecimal("1.00")).build()));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            AssessmentItemResponse.builder()
                    .id(speakingResponseId)
                    .sessionId(sessionId)
                    .paperItemId(speakingItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build(),
            AssessmentItemResponse.builder()
                    .id(listeningResponseId)
                    .sessionId(sessionId)
                    .paperItemId(listeningItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build()));

        when(rubricResultBandRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of(
            band("B1", "Trung cấp", "0.00", "6.49", 1),
            band("B2", "Trung cao cấp", "6.50", "7.99", 2),
            band("C1", "Cao cấp", "8.00", "9.00", 3)));
    }

    private AssessmentPaperSection section(UUID id, int order, String title, BigDecimal weight) {
        AssessmentPaperSection section = new AssessmentPaperSection();
        section.setId(id);
        section.setPaperId(paperId);
        section.setOrder(order);
        section.setTitle(title);
        section.setWeight(weight);
        return section;
    }

    private RubricResultBand band(String code, String name, String min, String max, int order) {
        RubricResultBand resultBand = new RubricResultBand();
        resultBand.setId(UUID.randomUUID());
        resultBand.setRubricVersionId(rubricVersionId);
        resultBand.setCode(code);
        resultBand.setName(name);
        resultBand.setScoreMin(new BigDecimal(min));
        resultBand.setScoreMax(new BigDecimal(max));
        resultBand.setOrder(order);
        return resultBand;
    }

    private AssessmentItemEvaluation evaluation(UUID responseId, String itemScore, boolean requiresHumanReview) {
        AssessmentItemEvaluation evaluation = new AssessmentItemEvaluation();
        evaluation.setId(UUID.randomUUID());
        evaluation.setResponseId(responseId);
        evaluation.setItemScore(new BigDecimal(itemScore));
        evaluation.setRequiresHumanReview(requiresHumanReview);
        return evaluation;
    }

    private void givenStoredScores(String speaking, String listening) {
        when(assessmentItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, speaking, false),
            evaluation(listeningResponseId, listening, false)));
    }

    @Test
    void should_weight_items_into_sections_then_sections_into_total() {
        givenStoredScores("6.00", "8.00");

        AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult result = calculator.calculate(sessionId);

        // Speaking 6.00*1.00 = 6.00; Listening 8.00*1.00 = 8.00. Total = (6.00*0.60 + 8.00*0.40) /
        // 1.00 = 6.80 -- a plain average would give 7.00.
        assertThat(result.totalScore()).isEqualByComparingTo("6.80");
        assertThat(result.sections()).extracting(section -> section.score())
            .containsExactly(new BigDecimal("6.00"), new BigDecimal("8.00"));
    }

    @Test
    void should_order_sections_by_their_declared_order() {
        givenStoredScores("6.00", "8.00");

        AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult result = calculator.calculate(sessionId);

        assertThat(result.sections()).extracting(section -> section.title())
            .containsExactly("Speaking", "Listening");
    }

    @Test
    void should_resolve_result_band_from_the_total() {
        givenStoredScores("6.00", "8.00");

        assertThat(calculator.calculate(sessionId).rubricResultBand().getCode()).isEqualTo("B2");
    }

    @Test
    void should_report_when_any_item_still_requires_human_review() {
        when(assessmentItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, "6.00", true),
            evaluation(listeningResponseId, "8.00", false)));

        assertThat(calculator.calculate(sessionId).anyRequiresHumanReview()).isTrue();
    }

    @Test
    void should_normalize_item_weights_within_a_section() {
        // One part has TWO items, each with weight 1.00 -- so the total weight inside the part is
        // 2.00, not 1.00. This is exactly the shape of the real assessment data, and where the old bug
        // sits.
        UUID secondItemId = UUID.randomUUID();
        UUID secondResponseId = UUID.randomUUID();
        when(assessmentPaperItemRepository.findByPaperId(paperId)).thenReturn(List.of(
            AssessmentPaperItem.builder().id(speakingItemId).sectionId(speakingSectionId).paperId(paperId).order(1).weight(new BigDecimal("1.00")).build(),
            AssessmentPaperItem.builder().id(secondItemId).sectionId(speakingSectionId).paperId(paperId).order(2).weight(new BigDecimal("1.00")).build(),
            AssessmentPaperItem.builder().id(listeningItemId).sectionId(listeningSectionId).paperId(paperId).order(1).weight(new BigDecimal("1.00")).build()));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            AssessmentItemResponse.builder()
                    .id(speakingResponseId)
                    .sessionId(sessionId)
                    .paperItemId(speakingItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build(),
            AssessmentItemResponse.builder()
                    .id(secondResponseId)
                    .sessionId(sessionId)
                    .paperItemId(secondItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build(),
            AssessmentItemResponse.builder()
                    .id(listeningResponseId)
                    .sessionId(sessionId)
                    .paperItemId(listeningItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build()));
        when(assessmentItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, "6.00", false),
            evaluation(secondResponseId, "8.00", false),
            evaluation(listeningResponseId, "8.00", false)));

        AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult result = calculator.calculate(sessionId);

        // Speaking = (6.00*1.00 + 8.00*1.00) / 2.00 = 7.00. The old version did not divide and gave
        // 14.00. Total = 7.00*0.60 + 8.00*0.40 = 7.40. The old version gave 11.60 -- over the 9.00
        // scale cap, and then NO grade band matches.
        assertThat(result.sections()).extracting(section -> section.score())
            .containsExactly(new BigDecimal("7.00"), new BigDecimal("8.00"));
        assertThat(result.totalScore()).isEqualByComparingTo("7.40");
        assertThat(result.rubricResultBand().getCode()).isEqualTo("B2");
    }

    @Test
    void should_fall_back_to_plain_mean_when_every_item_weight_in_a_section_is_zero() {
        when(assessmentPaperItemRepository.findByPaperId(paperId)).thenReturn(List.of(
            AssessmentPaperItem.builder().id(speakingItemId).sectionId(speakingSectionId).paperId(paperId).order(1).weight(new BigDecimal("0.00")).build(),
            AssessmentPaperItem.builder().id(listeningItemId).sectionId(listeningSectionId).paperId(paperId).order(1).weight(new BigDecimal("1.00")).build()));
        givenStoredScores("6.00", "8.00");

        AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult result = calculator.calculate(sessionId);

        // No item in the Speaking part declares a weight -> the items are equal, so take the plain
        // average = 6.00. The old version returned the multiplied sum, i.e. 0.00, which dragged the
        // total down to 3.20.
        assertThat(result.sections()).extracting(section -> section.score())
            .containsExactly(new BigDecimal("6.00"), new BigDecimal("8.00"));
        assertThat(result.totalScore()).isEqualByComparingTo("6.80");
    }

    @Test
    void should_reject_when_a_response_has_no_evaluation() {
        when(assessmentItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, "6.00", false)));

        assertThatThrownBy(() -> calculator.calculate(sessionId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("evaluation");
    }

    // ---- preview -----------------------------------------------------------

    @Test
    void should_preview_the_same_total_that_calculate_returns_when_nothing_is_overridden() {
        givenStoredScores("6.00", "8.00");

        assertThat(calculator.preview(sessionId, Map.of()).totalScore())
            .isEqualByComparingTo(calculator.calculate(sessionId).totalScore());
    }

    @Test
    void should_preview_the_total_that_calculate_would_return_for_the_overridden_scores() {
        givenStoredScores("6.00", "8.00");
        AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult previewed = calculator.preview(sessionId, Map.of(
            speakingResponseId, new BigDecimal("9.00"),
            listeningResponseId, new BigDecimal("7.00")));

        // This is the most important check of the preview endpoint: the number the teacher sees
        // before clicking Submit must equal the number the student receives after submission.
        givenStoredScores("9.00", "7.00");
        assertThat(previewed.totalScore()).isEqualByComparingTo(calculator.calculate(sessionId).totalScore());
        assertThat(previewed.totalScore()).isEqualByComparingTo("8.20");
    }

    @Test
    void should_fall_back_to_stored_scores_for_parts_not_yet_graded() {
        givenStoredScores("6.00", "8.00");

        // Grading in progress: only Speaking is entered, Listening keeps its AI score of 8.00.
        AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult previewed = calculator.preview(sessionId, Map.of(speakingResponseId, new BigDecimal("9.00")));

        assertThat(previewed.totalScore()).isEqualByComparingTo("8.60");
    }

    @Test
    void should_report_the_result_band_name_for_the_previewed_total() {
        givenStoredScores("6.00", "8.00");

        AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult previewed = calculator.preview(sessionId, Map.of(
            speakingResponseId, new BigDecimal("9.00"),
            listeningResponseId, new BigDecimal("9.00")));

        assertThat(previewed.resultBandName()).isEqualTo("Cao cấp");
    }

    @Test
    void should_expose_previewed_item_scores_per_paper_item() {
        givenStoredScores("6.00", "8.00");

        AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult previewed = calculator.preview(sessionId, Map.of(speakingResponseId, new BigDecimal("9.00")));

        assertThat(previewed.items()).extracting(itemScore -> itemScore.paperItemId())
            .containsExactly(speakingItemId, listeningItemId);
        assertThat(previewed.items()).extracting(itemScore -> itemScore.itemScore())
            .containsExactly(new BigDecimal("9.00"), new BigDecimal("8.00"));
    }
}
