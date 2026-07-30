package com.sep.vox.application.usecase.examevaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.sep.vox.application.port.input.command.examevaluation.RecordExamAttemptEvaluationCommand;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.application.port.input.usecase.exam.CompleteExamSessionGradingUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.RecordExamAttemptEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedPayloadDto;
import com.sep.vox.interfaces.kafka.mapper.RecordExamAttemptEvaluationCommandMapper;

/**
 * Điểm người đã chốt phải thắng AI.
 *
 * <p>Không có optimistic lock trên exam_item_evaluations: nếu consumer cứ ghi đè,
 * một lượt chấm lại của AI (re-run hay replay Kafka) sẽ nuốt mất bản HUMAN
 * FINALIZED của giáo viên và xoá cả criterion scores — mất im lặng, không ai phát
 * hiện. Đây là test tái hiện đúng kịch bản đó.
 */
public class RecordExamAttemptEvaluationHumanGuardTests {

    private ExamItemResponseRepository examItemResponseRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamRepository examRepository;
    private AssessmentPolicyRepository assessmentPolicyRepository;
    private RubricVersionRepository rubricVersionRepository;
    private UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase;
    private JsonSerializationPort jsonSerializationPort;
    private RecordExamAttemptEvaluationUseCase useCase;

    private final UUID responseId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        examItemEvaluationTurnRepository = mock(ExamItemEvaluationTurnRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examRepository = mock(ExamRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        upsertExamCandidateResultUseCase = mock(UpsertExamCandidateResultUseCase.class);
        completeExamSessionGradingUseCase = mock(CompleteExamSessionGradingUseCase.class);
        jsonSerializationPort = mock(JsonSerializationPort.class);

        var transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenAnswer(
            invocation -> (TransactionStatus) new SimpleTransactionStatus());

        useCase = new RecordExamAttemptEvaluationUseCase(
            examItemResponseRepository, examItemEvaluationRepository, examItemCriterionScoreRepository,
            examItemEvaluationTurnRepository, rubricCriterionRepository, examSessionRepository, examRepository,
            assessmentPolicyRepository, rubricVersionRepository, upsertExamCandidateResultUseCase,
            completeExamSessionGradingUseCase,
            transactionManager, jsonSerializationPort, new ConfidenceReviewCalculator());

        when(examItemResponseRepository.findById(responseId)).thenReturn(Optional.of(
            new ExamItemResponse(responseId, sessionId, paperItemId, null, null, null, null, null)));
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of());

        // Chuỗi session -> exam -> policy -> rubric chỉ cần thiết cho các nhánh KHÔNG
        // bị chặn; nhánh bị chặn phải thoát trước khi chạm tới nó.
        var session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        var exam = new Exam();
        exam.setId(examId);
        exam.setAssessmentPolicyId(policyId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        var policy = new AssessmentPolicy();
        policy.setId(policyId);
        policy.setRubricVersionId(rubricVersionId);
        when(assessmentPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(rubricVersionRepository.findById(rubricVersionId))
            .thenReturn(Optional.of(new com.sep.vox.domain.model.rubric.RubricVersion()));
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of());
    }

    private RecordExamAttemptEvaluationCommand command() {
        var payload = new ExamAttemptEvaluationCompletedPayloadDto(
            List.of(), Map.of(), null, null, "Khá", List.of(), "gpt-4o", "v1", null);
        var event = new ExamAttemptEvaluationCompletedEventDto(
            "exam.attempt.evaluation.completed", 1, null, responseId.toString(), null, payload);
        return RecordExamAttemptEvaluationCommandMapper.toCommand(event);
    }

    private void givenLatestEvaluation(
            ExamEvaluationEngineType engineType, ExamItemEvaluationStatus status) {
        var evaluation = new ExamItemEvaluation();
        evaluation.setId(UUID.randomUUID());
        evaluation.setResponseId(responseId);
        evaluation.setEngineType(engineType);
        evaluation.setStatus(status);
        evaluation.setItemScore(new BigDecimal("8.00"));
        when(examItemEvaluationRepository.findLatestByResponseId(responseId))
            .thenReturn(Optional.of(evaluation));
    }

    @Test
    void should_skip_ai_regrade_when_a_human_evaluation_is_already_finalized() {
        givenLatestEvaluation(ExamEvaluationEngineType.HUMAN, ExamItemEvaluationStatus.FINALIZED);

        useCase.execute(command());

        verify(examItemEvaluationRepository, never()).save(any());
        verify(examItemCriterionScoreRepository, never()).deleteByEvaluationIdIn(anyCollection());
        verify(examItemEvaluationTurnRepository, never()).deleteByEvaluationIdIn(anyCollection());
    }

    @Test
    void should_still_overwrite_a_previous_ai_evaluation() {
        givenLatestEvaluation(ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.AUTO_GRADED);
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        // Chỉ điểm NGƯỜI mới được bảo vệ; AI chấm lại chính bản AI của nó là bình thường.
        verify(examItemEvaluationRepository).save(any());
    }

    @Test
    void should_still_record_the_first_ai_evaluation() {
        when(examItemEvaluationRepository.findLatestByResponseId(responseId)).thenReturn(Optional.empty());
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            var evaluation = (ExamItemEvaluation) invocation.getArgument(0);
            evaluation.setId(UUID.randomUUID());
            return evaluation;
        });

        useCase.execute(command());

        verify(examItemEvaluationRepository).save(any());
    }

    @Test
    void should_keep_the_stored_human_score_untouched() {
        givenLatestEvaluation(ExamEvaluationEngineType.HUMAN, ExamItemEvaluationStatus.FINALIZED);

        useCase.execute(command());

        var stored = examItemEvaluationRepository.findLatestByResponseId(responseId).orElseThrow();
        assertThat(stored.getItemScore()).isEqualByComparingTo("8.00");
        assertThat(stored.getStatus()).isEqualTo(ExamItemEvaluationStatus.FINALIZED);
    }
}
