package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator.PreviewedExamSessionResult;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.RegradeResultUseCase;
import com.sep.vox.application.response.input.examgrading.GradingPreviewResponse;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

public class PreviewGradingUseCaseTests {

    private ExamSessionResultCalculator examSessionResultCalculator;
    private ExamGradingAccessService examGradingAccessService;
    private ExamItemResponseRepository examItemResponseRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private RubricVersionRepository rubricVersionRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private GradingItemScoreResolver resolver;
    private PreviewGradingUseCase useCase;

    private final UUID assignmentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID fluencyId = UUID.randomUUID();
    private final UUID pronunciationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examSessionResultCalculator = mock(ExamSessionResultCalculator.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        upsertExamCandidateResultUseCase = mock(UpsertExamCandidateResultUseCase.class);

        resolver = new GradingItemScoreResolver(
            examItemResponseRepository, rubricCriterionRepository, rubricVersionRepository);
        useCase = new PreviewGradingUseCase(
            examSessionResultCalculator, examGradingAccessService, resolver);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(examGradingAccessService.load(assignmentId)).thenReturn(context());
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            new ExamItemResponse(responseId, sessionId, paperItemId, null, null, null, null, null)));
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of(
            criterion(fluencyId, "FLU", "Trôi chảy", new BigDecimal("0.60")),
            criterion(pronunciationId, "PRO", "Phát âm", new BigDecimal("0.40"))));
        when(rubricVersionRepository.findById(rubricVersionId)).thenReturn(Optional.of(rubricVersion()));
        when(examSessionResultCalculator.preview(eq(sessionId), any())).thenReturn(
            new PreviewedExamSessionResult(new BigDecimal("7.20"), "Trung cao cấp", List.of(), List.of()));
    }

    private RubricCriterion criterion(UUID id, String code, String name, BigDecimal weight) {
        return new RubricCriterion(id, rubricVersionId, null, code, name, null, null, weight,
            new BigDecimal("0.00"), new BigDecimal("9.00"), 1, true, null, null, null, null);
    }

    private RubricVersion rubricVersion() {
        RubricVersion version = new RubricVersion();
        version.setTotalScoreMethod(RubricTotalScoreMethod.WEIGHTED_AVERAGE);
        version.setScoringScaleMin(BigDecimal.ZERO);
        version.setScoringScaleMax(BigDecimal.TEN);
        return version;
    }

    private GradingContext context() {
        ExamGradingAssignment assignment = ExamGradingAssignment.open(candidateResultId, teacherId,
            GradingRoundType.INITIAL, null, null, Instant.now(), null, null);
        assignment.setId(assignmentId);

        ExamCandidateResult candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setSessionId(sessionId);
        candidateResult.setRubricVersionId(rubricVersionId);
        candidateResult.setStatus(ExamCandidateResultStatus.PENDING_REVIEW);

        ExamSession session = new ExamSession();
        session.setId(sessionId);

        return new GradingContext(assignment, candidateResult, session, UUID.randomUUID(), "IELTS Speaking Mock", ExamKind.CENTRALIZED);
    }

    private SubmitGradingCommand command(String fluency, String pronunciation) {
        return new SubmitGradingCommand(assignmentId, List.of(
            new SubmitGradingCommand.ItemGrade(paperItemId, List.of(
                new SubmitGradingCommand.CriterionScoreItem(fluencyId, new BigDecimal(fluency), null),
                new SubmitGradingCommand.CriterionScoreItem(pronunciationId, new BigDecimal(pronunciation), null)
            ), null)));
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, BigDecimal> captureOverrides() {
        ArgumentCaptor<Map<UUID, BigDecimal>> captor = (ArgumentCaptor<Map<UUID, BigDecimal>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(examSessionResultCalculator).preview(eq(sessionId), captor.capture());
        return captor.getValue();
    }

    @Test
    void should_return_total_and_band_from_the_calculator() {
        GradingPreviewResponse response = useCase.execute(command("8.00", "6.00"));

        assertThat(response.totalScore()).isEqualByComparingTo("7.20");
        assertThat(response.resultBandName()).isEqualTo("Trung cao cấp");
    }

    @Test
    void should_not_write_anything() {
        useCase.execute(command("8.00", "6.00"));

        // Preview là POST nhưng chỉ đọc — nếu nó ghi, giáo viên "xem thử" là đã chấm.
        verifyNoInteractions(examItemEvaluationRepository);
        verifyNoInteractions(examItemCriterionScoreRepository);
        verifyNoInteractions(examSessionRepository);
        verifyNoInteractions(examGradingAssignmentRepository);
        verifyNoInteractions(upsertExamCandidateResultUseCase);
    }

    @Test
    void should_hand_the_calculator_the_same_item_score_that_submitting_would_persist() {
        // The most important test of this endpoint: the number the teacher sees before clicking
        // Submit must be exactly the number that is written when they submit. Same command, same
        // resolver.
        useCase.execute(command("8.00", "6.00"));
        BigDecimal previewedItemScore = captureOverrides().get(responseId);

        // GradingActionSupport is mocked: authorization / audit / closing the assignment already
        // have their own tests; here we only need that both paths go through the same resolver.
        GradingActionSupport gradingActionSupport = mock(com.sep.vox.application.port.input.service.GradingActionSupport.class);
        ExamGradingAccessService.GradingContext context = context();
        when(gradingActionSupport.prepare(any(), any(), any())).thenReturn(
            new com.sep.vox.application.port.input.service.GradingActionSupport.PreparedAction(
                context, teacherId, GradingRoundType.INITIAL, GradingOutcome.REGRADED, null, null));

        RegradeResultUseCase regradeUseCase = new RegradeResultUseCase(
            gradingActionSupport, resolver, examItemEvaluationRepository, examItemCriterionScoreRepository,
            examSessionRepository, upsertExamCandidateResultUseCase);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            ExamItemEvaluation evaluation = (ExamItemEvaluation) invocation.getArgument(0);
            evaluation.setId(UUID.randomUUID());
            return evaluation;
        });
        ExamCandidateResult recalculated = new ExamCandidateResult();
        recalculated.setStatus(ExamCandidateResultStatus.RELEASED);
        recalculated.setTotalScore(new BigDecimal("7.20"));
        when(upsertExamCandidateResultUseCase.execute(eq(sessionId), any())).thenReturn(recalculated);

        regradeUseCase.execute(command("8.00", "6.00"));

        ArgumentCaptor<ExamItemEvaluation> captor = ArgumentCaptor.forClass(ExamItemEvaluation.class);
        verify(examItemEvaluationRepository).save(captor.capture());
        assertThat(previewedItemScore).isEqualByComparingTo(captor.getValue().getItemScore());
    }

    @Test
    void should_reject_when_teacher_is_not_the_assigned_one() {
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examGradingAccessService).authorizeAssignedTeacher(any(), eq(teacherId));

        assertThatThrownBy(() -> useCase.execute(command("8.00", "6.00")))
            .isInstanceOf(ForbiddenException.class);

        verify(examSessionResultCalculator, never()).preview(any(), any());
    }

    @Test
    void should_reject_score_outside_criterion_range_before_calculating() {
        assertThatThrownBy(() -> useCase.execute(command("9.50", "6.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0.00 - 9.00");

        verify(examSessionResultCalculator, never()).preview(any(), any());
    }

    @Test
    void should_allow_previewing_only_some_parts() {
        // The submission has 2 parts and the teacher has graded only 1 -- the preview does NOT
        // require full coverage (unlike /grade), so the total can build up while grading. The
        // calculator takes the old score for the part not yet entered.
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            new ExamItemResponse(responseId, sessionId, paperItemId, null, null, null, null, null),
            new ExamItemResponse(UUID.randomUUID(), sessionId, UUID.randomUUID(), null, null, null, null, null)));

        GradingPreviewResponse response = useCase.execute(command("8.00", "6.00"));

        assertThat(response.totalScore()).isEqualByComparingTo("7.20");
        verify(examSessionResultCalculator).preview(eq(sessionId), any());
    }
}
