package com.sep.vox.application.usecase.examevaluation;

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

import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.examevaluation.ViewExamItemResponseEvaluationUseCase;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamItemEvaluationTurn;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

/**
 * Sau khi giáo viên chấm lại, bản đang hiệu lực là bản HUMAN — không có turn, không có
 * signals/validity/confidence. Đọc mọi thứ theo bản ấy là màn kết quả của học sinh mất
 * nội dung câu hỏi, audio, transcript và toàn bộ phân tích của AI.
 *
 * <p>Ranh giới cần giữ đúng: điểm và rationale theo tiêu chí là của GIÁO VIÊN, còn lượt
 * nói và bằng chứng là của bản AI.
 */
class ViewExamItemResponseEvaluationUseCaseTests {

    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final UUID PAPER_ITEM_ID = UUID.randomUUID();
    private static final UUID AI_EVALUATION_ID = UUID.randomUUID();
    private static final UUID HUMAN_EVALUATION_ID = UUID.randomUUID();
    private static final UUID CRITERION_ID = UUID.randomUUID();

    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private com.sep.vox.domain.repository.ExamItemResponseTurnRepository examItemResponseTurnRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private ExamResultAccessService examResultAccessService;
    private ViewExamItemResponseEvaluationUseCase useCase;

    @BeforeEach
    void setUp() {
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        examItemEvaluationTurnRepository = mock(ExamItemEvaluationTurnRepository.class);
        examItemResponseTurnRepository = mock(com.sep.vox.domain.repository.ExamItemResponseTurnRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        examResultAccessService = mock(ExamResultAccessService.class);
        useCase = new ViewExamItemResponseEvaluationUseCase(
            examItemEvaluationRepository,
            examItemCriterionScoreRepository,
            examItemEvaluationTurnRepository,
            examItemResponseTurnRepository,
            rubricCriterionRepository,
            examResultAccessService,
            new FakeJsonSerializationPort()
        );
        when(rubricCriterionRepository.findById(any())).thenReturn(Optional.empty());
        when(examItemEvaluationTurnRepository.findByEvaluationId(any())).thenReturn(List.of());
        // Không có lượt gốc: các ca sẵn có kiểm bản AI, nhánh lùi phải đứng yên.
        when(examItemResponseTurnRepository.findByExamItemResponseId(any())).thenReturn(List.of());
        when(examItemCriterionScoreRepository.findByEvaluationId(any())).thenReturn(List.of());
    }

    @Test
    void should_keep_ai_evidence_when_latest_evaluation_is_human() {
        givenHumanRegradedOverAi();

        var response = useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

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

        var response = useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

        // Phán quyết vẫn là của giáo viên — khối ai chỉ là ngữ cảnh, không được lấn lên.
        assertThat(response.engineType()).isEqualTo("HUMAN");
        assertThat(response.itemScore()).isEqualByComparingTo("8.00");
        assertThat(response.feedbackSummary()).isEqualTo("GV: đạt yêu cầu");
        assertThat(response.overallConfidence()).isNull();
    }

    @Test
    void should_return_teacher_criteria_when_latest_evaluation_is_human() {
        givenHumanRegradedOverAi();
        when(examItemCriterionScoreRepository.findByEvaluationId(HUMAN_EVALUATION_ID)).thenReturn(List.of(
            new ExamItemCriterionScore(UUID.randomUUID(), HUMAN_EVALUATION_ID, CRITERION_ID,
                new BigDecimal("8.00"), new BigDecimal("8.00"), "GV: trôi chảy")));
        when(examItemCriterionScoreRepository.findByEvaluationId(AI_EVALUATION_ID)).thenReturn(List.of(
            new ExamItemCriterionScore(UUID.randomUUID(), AI_EVALUATION_ID, CRITERION_ID,
                new BigDecimal("6.00"), new BigDecimal("6.00"), "AI: còn ngập ngừng")));

        var response = useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

        assertThat(response.criteria()).hasSize(1);
        assertThat(response.criteria().get(0).finalScore()).isEqualByComparingTo("8.00");
        assertThat(response.criteria().get(0).rationale()).isEqualTo("GV: trôi chảy");
    }

    @Test
    void should_read_turns_from_ai_evaluation_when_human_row_has_none() {
        givenHumanRegradedOverAi();
        when(examItemEvaluationTurnRepository.findByEvaluationId(AI_EVALUATION_ID)).thenReturn(List.of(
            new ExamItemEvaluationTurn(UUID.randomUUID(), AI_EVALUATION_ID, 1, TurnType.MAIN,
                "Describe a place you like", "https://audio", "I like...", 12, 30, 0.9, null, null)));

        var response = useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

        // Nội dung câu hỏi trên màn học sinh được suy ra từ turn — mất turn là mất câu hỏi.
        assertThat(response.turns()).hasSize(1);
        assertThat(response.turns().get(0).promptText()).isEqualTo("Describe a place you like");
        verify(examItemEvaluationTurnRepository, never()).findByEvaluationId(HUMAN_EVALUATION_ID);
    }

    @Test
    void should_not_query_ai_evaluation_when_latest_is_already_ai() {
        var ai = aiEvaluation();
        when(examItemEvaluationRepository.findLatestByResponseId(RESPONSE_ID)).thenReturn(Optional.of(ai));

        var response = useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

        verify(examItemEvaluationRepository, never()).findLatestAiByResponseId(any());
        assertThat(response.ai()).isNotNull();
        assertThat(response.ai().evaluationId()).isEqualTo(AI_EVALUATION_ID);
    }

    @Test
    void should_return_null_ai_context_when_response_has_no_ai_evaluation() {
        when(examItemEvaluationRepository.findLatestByResponseId(RESPONSE_ID))
            .thenReturn(Optional.of(humanEvaluation()));
        when(examItemEvaluationRepository.findLatestAiByResponseId(RESPONSE_ID)).thenReturn(Optional.empty());

        var response = useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

        assertThat(response.ai()).isNull();
        assertThat(response.turns()).isEmpty();
    }

    @Test
    void should_check_result_visibility_before_returning_evaluation() {
        givenHumanRegradedOverAi();

        useCase.execute(new ViewExamItemResponseEvaluationQuery(RESPONSE_ID));

        verify(examResultAccessService).requireCandidateVisibleResponse(RESPONSE_ID);
    }

    private void givenHumanRegradedOverAi() {
        when(examItemEvaluationRepository.findLatestByResponseId(RESPONSE_ID))
            .thenReturn(Optional.of(humanEvaluation()));
        when(examItemEvaluationRepository.findLatestAiByResponseId(RESPONSE_ID))
            .thenReturn(Optional.of(aiEvaluation()));
    }

    /** Bản AI sau khi bị chấm lại: SUPERSEDED nhưng dữ liệu còn nguyên. */
    private ExamItemEvaluation aiEvaluation() {
        return new ExamItemEvaluation(
            AI_EVALUATION_ID, RESPONSE_ID, PAPER_ITEM_ID, ExamEvaluationEngineType.AI_SINGLE,
            "gpt-x", 1, null, new BigDecimal("6.00"), new BigDecimal("6.00"),
            new BigDecimal("0.82"), true, "LOW_CONFIDENCE", false, false,
            null, "{\"ruleResults\":[]}", "AI: phát âm ổn", "[\"nói chậm lại\"]", "v3",
            ExamItemEvaluationStatus.SUPERSEDED, Instant.now().minus(1, ChronoUnit.HOURS));
    }

    /** Bản chấm tay: mọi cột AI đều null, không sinh turn nào. */
    private ExamItemEvaluation humanEvaluation() {
        return new ExamItemEvaluation(
            HUMAN_EVALUATION_ID, RESPONSE_ID, PAPER_ITEM_ID, ExamEvaluationEngineType.HUMAN,
            "HUMAN", null, UUID.randomUUID(), new BigDecimal("8.00"), new BigDecimal("8.00"),
            null, false, null, false, false,
            null, null, "GV: đạt yêu cầu", null, null,
            ExamItemEvaluationStatus.FINALIZED, Instant.now());
    }
}
