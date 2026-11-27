package com.sep.vox.application.usecase.assessmentgrading;

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
import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RegradeResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingPreviewResponse;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

public class PreviewGradingUseCaseTests {

    private AssessmentSessionResultCalculator assessmentSessionResultCalculator;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private RubricVersionRepository rubricVersionRepository;
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
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
        assessmentSessionResultCalculator = mock(AssessmentSessionResultCalculator.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        assessmentItemCriterionScoreRepository = mock(AssessmentItemCriterionScoreRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        upsertAssessmentCandidateResultUseCase = mock(UpsertAssessmentCandidateResultUseCase.class);

        resolver = new GradingItemScoreResolver(
            assessmentItemResponseRepository, rubricCriterionRepository, rubricVersionRepository);
        useCase = new PreviewGradingUseCase(
            assessmentSessionResultCalculator, assessmentGradingAccessService, resolver);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(assessmentGradingAccessService.load(assignmentId)).thenReturn(context());
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            AssessmentItemResponse.builder()
                    .id(responseId)
                    .sessionId(sessionId)
                    .paperItemId(paperItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build()));
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of(
            criterion(fluencyId, "FLU", "Trôi chảy", new BigDecimal("0.60")),
            criterion(pronunciationId, "PRO", "Phát âm", new BigDecimal("0.40"))));
        when(rubricVersionRepository.findById(rubricVersionId)).thenReturn(Optional.of(rubricVersion()));
        when(assessmentSessionResultCalculator.preview(eq(sessionId), any())).thenReturn(
            new PreviewedAssessmentSessionResult(new BigDecimal("7.20"), "Trung cao cấp", List.of(), List.of()));
    }

    private RubricCriterion criterion(UUID id, String code, String name, BigDecimal weight) {
        return RubricCriterion.builder()
                .id(id)
                .rubricVersionId(rubricVersionId)
                .frameworkCriterionId(null)
                .code(code)
                .name(name)
                .description(null)
                .examples(null)
                .weight(weight)
                .minScore(new BigDecimal("0.00"))
                .maxScore(new BigDecimal("9.00"))
                .order(1)
                .required(true)
                .build();
    }

    private RubricVersion rubricVersion() {
        RubricVersion version = new RubricVersion();
        version.setTotalScoreMethod(RubricTotalScoreMethod.WEIGHTED_AVERAGE);
        version.setScoringScaleMin(BigDecimal.ZERO);
        version.setScoringScaleMax(BigDecimal.TEN);
        return version;
    }

    private GradingContext context() {
        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(candidateResultId, teacherId,
            GradingRoundType.INITIAL, null, null, Instant.now(), null, null);
        assignment.setId(assignmentId);

        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setSessionId(sessionId);
        candidateResult.setRubricVersionId(rubricVersionId);
        candidateResult.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);

        return new GradingContext(assignment, candidateResult, session, UUID.randomUUID(), "IELTS Speaking Mock", AssessmentKind.CENTRALIZED);
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
        verify(assessmentSessionResultCalculator).preview(eq(sessionId), captor.capture());
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

        // Preview is a POST but read-only -- if it wrote, a teacher "trying it out" would already have graded.
        verifyNoInteractions(assessmentItemEvaluationRepository);
        verifyNoInteractions(assessmentItemCriterionScoreRepository);
        verifyNoInteractions(assessmentSessionRepository);
        verifyNoInteractions(assessmentGradingAssignmentRepository);
        verifyNoInteractions(upsertAssessmentCandidateResultUseCase);
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
        AssessmentGradingAccessService.GradingContext context = context();
        when(gradingActionSupport.prepare(any(), any(), any())).thenReturn(
            new com.sep.vox.application.port.input.service.GradingActionSupport.PreparedAction(
                context, teacherId, GradingRoundType.INITIAL, GradingOutcome.REGRADED, null, null));

        RegradeResultUseCase regradeUseCase = new RegradeResultUseCase(
            gradingActionSupport, resolver, assessmentItemEvaluationRepository, assessmentItemCriterionScoreRepository,
            assessmentSessionRepository, upsertAssessmentCandidateResultUseCase);
        when(assessmentItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        when(assessmentItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            AssessmentItemEvaluation evaluation = (AssessmentItemEvaluation) invocation.getArgument(0);
            evaluation.setId(UUID.randomUUID());
            return evaluation;
        });
        AssessmentCandidateResult recalculated = new AssessmentCandidateResult();
        recalculated.setStatus(AssessmentCandidateResultStatus.RELEASED);
        recalculated.setTotalScore(new BigDecimal("7.20"));
        when(upsertAssessmentCandidateResultUseCase.execute(eq(sessionId), any())).thenReturn(recalculated);

        regradeUseCase.execute(command("8.00", "6.00"));

        ArgumentCaptor<AssessmentItemEvaluation> captor = ArgumentCaptor.forClass(AssessmentItemEvaluation.class);
        verify(assessmentItemEvaluationRepository).save(captor.capture());
        assertThat(previewedItemScore).isEqualByComparingTo(captor.getValue().getItemScore());
    }

    @Test
    void should_reject_when_teacher_is_not_the_assigned_one() {
        org.mockito.Mockito.doThrow(new ForbiddenException("SECURITY"))
            .when(assessmentGradingAccessService).authorizeAssignedTeacher(any(), eq(teacherId));

        assertThatThrownBy(() -> useCase.execute(command("8.00", "6.00")))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentSessionResultCalculator, never()).preview(any(), any());
    }

    @Test
    void should_reject_score_outside_criterion_range_before_calculating() {
        assertThatThrownBy(() -> useCase.execute(command("9.50", "6.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0.00 - 9.00");

        verify(assessmentSessionResultCalculator, never()).preview(any(), any());
    }

    @Test
    void should_allow_previewing_only_some_parts() {
        // The submission has 2 parts and the teacher has graded only 1 -- the preview does NOT
        // require full coverage (unlike /grade), so the total can build up while grading. The
        // calculator takes the old score for the part not yet entered.
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            AssessmentItemResponse.builder()
                    .id(responseId)
                    .sessionId(sessionId)
                    .paperItemId(paperItemId)
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build(),
            AssessmentItemResponse.builder()
                    .id(UUID.randomUUID())
                    .sessionId(sessionId)
                    .paperItemId(UUID.randomUUID())
                    .audioUrl(null)
                    .durationSeconds(null)
                    .transcript(null)
                    .terminationReason(null)
                    .submittedAt(null)
                    .build()));

        GradingPreviewResponse response = useCase.execute(command("8.00", "6.00"));

        assertThat(response.totalScore()).isEqualByComparingTo("7.20");
        verify(assessmentSessionResultCalculator).preview(eq(sessionId), any());
    }
}
