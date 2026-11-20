package com.sep.vox.application.usecase.assessmentevaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ViewAssessmentItemResponseEvaluationUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationDetailsResponse;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

/**
 * Sau khi giáo viên chấm lại, bản đang hiệu lực là bản HUMAN — không có turn, không có
 * signals/validity/confidence. Đọc mọi thứ theo bản ấy là màn kết quả của học sinh mất
 * nội dung câu hỏi, audio, transcript và toàn bộ phân tích của AI.
 *
 * <p>Ranh giới cần giữ đúng: điểm và rationale theo tiêu chí là của GIÁO VIÊN, còn lượt
 * nói và bằng chứng là của bản AI.
 */
class ViewAssessmentItemResponseEvaluationUseCaseTests {

    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final UUID PAPER_ITEM_ID = UUID.randomUUID();
    private static final UUID AI_EVALUATION_ID = UUID.randomUUID();
    private static final UUID HUMAN_EVALUATION_ID = UUID.randomUUID();
    private static final UUID CRITERION_ID = UUID.randomUUID();

    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private AssessmentItemEvaluationTurnRepository assessmentItemEvaluationTurnRepository;
    private com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private AssessmentResultAccessService assessmentResultAccessService;
    private ViewAssessmentItemResponseEvaluationUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        assessmentItemCriterionScoreRepository = mock(AssessmentItemCriterionScoreRepository.class);
        assessmentItemEvaluationTurnRepository = mock(AssessmentItemEvaluationTurnRepository.class);
        assessmentItemResponseTurnRepository = mock(com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        assessmentResultAccessService = mock(AssessmentResultAccessService.class);
        useCase = new ViewAssessmentItemResponseEvaluationUseCase(
            assessmentItemEvaluationRepository,
            assessmentItemCriterionScoreRepository,
            assessmentItemEvaluationTurnRepository,
            assessmentItemResponseTurnRepository,
            rubricCriterionRepository,
            assessmentResultAccessService,
            new FakeJsonSerializationPort()
        );
        when(rubricCriterionRepository.findById(any())).thenReturn(Optional.empty());
        when(assessmentItemEvaluationTurnRepository.findByEvaluationId(any())).thenReturn(List.of());
        // Không có lượt gốc: các ca sẵn có kiểm bản AI, nhánh lùi phải đứng yên.
        when(assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(any())).thenReturn(List.of());
        when(assessmentItemCriterionScoreRepository.findByEvaluationId(any())).thenReturn(List.of());
    }

    @Test
    void should_keep_ai_evidence_when_latest_evaluation_is_human() {
        givenHumanRegradedOverAi();

        AssessmentItemEvaluationDetailsResponse response = useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        assertThat(response.ai()).isNotNull();
        assertThat(response.ai().evaluationId()).isEqualTo(AI_EVALUATION_ID);
        assertThat(response.ai().overallConfidence()).isEqualByComparingTo("0.82");
        assertThat(response.ai().validity()).isEqualTo("{\"ruleResults\":[]}");
        assertThat(response.ai().suggestions()).isEqualTo("[\"nói chậm lại\"]");
        assertThat(response.ai().feedbackSummary()).isEqualTo("AI: phát âm ổn");
        assertThat(response.ai().gradedByModel()).isEqualTo("gpt-x");
    }

    @Test
    void should_return_teacher_verdict_at_top_level_when_latest_evaluation_is_human() {
        givenHumanRegradedOverAi();

        AssessmentItemEvaluationDetailsResponse response = useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        // The verdict still belongs to the teacher -- the AI block is only context and must not
        // encroach on it.
        assertThat(response.engineType()).isEqualTo("HUMAN");
        assertThat(response.itemScore()).isEqualByComparingTo("8.00");
        assertThat(response.feedbackSummary()).isEqualTo("GV: đạt yêu cầu");
        assertThat(response.overallConfidence()).isNull();
    }

    @Test
    void should_return_teacher_criteria_when_latest_evaluation_is_human() {
        givenHumanRegradedOverAi();
        when(assessmentItemCriterionScoreRepository.findByEvaluationId(HUMAN_EVALUATION_ID)).thenReturn(List.of(
            new AssessmentItemCriterionScore(UUID.randomUUID(), HUMAN_EVALUATION_ID, CRITERION_ID,
                new BigDecimal("8.00"), new BigDecimal("8.00"), "GV: trôi chảy")));
        when(assessmentItemCriterionScoreRepository.findByEvaluationId(AI_EVALUATION_ID)).thenReturn(List.of(
            new AssessmentItemCriterionScore(UUID.randomUUID(), AI_EVALUATION_ID, CRITERION_ID,
                new BigDecimal("6.00"), new BigDecimal("6.00"), "AI: còn ngập ngừng")));

        AssessmentItemEvaluationDetailsResponse response = useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        assertThat(response.criteria()).hasSize(1);
        assertThat(response.criteria().get(0).finalScore()).isEqualByComparingTo("8.00");
        assertThat(response.criteria().get(0).rationale()).isEqualTo("GV: trôi chảy");
    }

    @Test
    void should_read_turns_from_ai_evaluation_when_human_row_has_none() {
        givenHumanRegradedOverAi();
        when(assessmentItemEvaluationTurnRepository.findByEvaluationId(AI_EVALUATION_ID)).thenReturn(List.of(
            new AssessmentItemEvaluationTurn(UUID.randomUUID(), AI_EVALUATION_ID, 1, TurnType.MAIN,
                "Describe a place you like", "https://audio", "I like...", 12, 30, 0.9, null, null)));

        AssessmentItemEvaluationDetailsResponse response = useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        // The question content on the student screen is derived from the turn -- losing the turn
        // means losing the question.
        assertThat(response.turns()).hasSize(1);
        assertThat(response.turns().get(0).promptText()).isEqualTo("Describe a place you like");
        verify(assessmentItemEvaluationTurnRepository, never()).findByEvaluationId(HUMAN_EVALUATION_ID);
    }

    @Test
    void should_not_query_ai_evaluation_when_latest_is_already_ai() {
        AssessmentItemEvaluation ai = aiEvaluation();
        when(assessmentItemEvaluationRepository.findLatestByResponseId(RESPONSE_ID)).thenReturn(Optional.of(ai));

        AssessmentItemEvaluationDetailsResponse response = useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        verify(assessmentItemEvaluationRepository, never()).findLatestAiByResponseId(any());
        assertThat(response.ai()).isNotNull();
        assertThat(response.ai().evaluationId()).isEqualTo(AI_EVALUATION_ID);
    }

    @Test
    void should_return_null_ai_context_when_response_has_no_ai_evaluation() {
        when(assessmentItemEvaluationRepository.findLatestByResponseId(RESPONSE_ID))
            .thenReturn(Optional.of(humanEvaluation()));
        when(assessmentItemEvaluationRepository.findLatestAiByResponseId(RESPONSE_ID)).thenReturn(Optional.empty());

        AssessmentItemEvaluationDetailsResponse response = useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        assertThat(response.ai()).isNull();
        assertThat(response.turns()).isEmpty();
    }

    @Test
    void should_check_result_visibility_before_returning_evaluation() {
        givenHumanRegradedOverAi();

        useCase.execute(new ViewAssessmentItemResponseEvaluationQuery(RESPONSE_ID));

        verify(assessmentResultAccessService).requireCandidateVisibleResponse(RESPONSE_ID);
    }

    private void givenHumanRegradedOverAi() {
        when(assessmentItemEvaluationRepository.findLatestByResponseId(RESPONSE_ID))
            .thenReturn(Optional.of(humanEvaluation()));
        when(assessmentItemEvaluationRepository.findLatestAiByResponseId(RESPONSE_ID))
            .thenReturn(Optional.of(aiEvaluation()));
    }

    /** Bản AI sau khi bị chấm lại: SUPERSEDED nhưng dữ liệu còn nguyên. */
    private AssessmentItemEvaluation aiEvaluation() {
        return new AssessmentItemEvaluation(
            AI_EVALUATION_ID, RESPONSE_ID, PAPER_ITEM_ID, AssessmentEvaluationEngineType.AI_SINGLE,
            "gpt-x", 1, null, new BigDecimal("6.00"), new BigDecimal("6.00"),
            new BigDecimal("0.82"), true, "LOW_CONFIDENCE", false, false,
            null, "{\"ruleResults\":[]}", "AI: phát âm ổn", "[\"nói chậm lại\"]", "v3",
            AssessmentItemEvaluationStatus.SUPERSEDED, Instant.now().minus(1, ChronoUnit.HOURS));
    }

    /** Bản chấm tay: mọi cột AI đều null, không sinh turn nào. */
    private AssessmentItemEvaluation humanEvaluation() {
        return new AssessmentItemEvaluation(
            HUMAN_EVALUATION_ID, RESPONSE_ID, PAPER_ITEM_ID, AssessmentEvaluationEngineType.HUMAN,
            "HUMAN", null, UUID.randomUUID(), new BigDecimal("8.00"), new BigDecimal("8.00"),
            null, false, null, false, false,
            null, null, "GV: đạt yêu cầu", null, null,
            AssessmentItemEvaluationStatus.FINALIZED, Instant.now());
    }
}
