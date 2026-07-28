package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.SubmitGradingUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
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

public class SubmitGradingUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private RubricVersionRepository rubricVersionRepository;
    private UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private ExamGradingAccessService examGradingAccessService;
    private SubmitGradingUseCase useCase;

    private final UUID assignmentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID fluencyId = UUID.randomUUID();
    private final UUID pronunciationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        upsertExamCandidateResultUseCase = mock(UpsertExamCandidateResultUseCase.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);

        // Resolver thật (không mock): công thức điểm có trọng số là thứ các test này
        // phải chứng minh, mock nó đi thì chẳng còn gì để kiểm.
        var resolver = new GradingItemScoreResolver(
            examItemResponseRepository, rubricCriterionRepository, rubricVersionRepository);
        useCase = new SubmitGradingUseCase(
            examGradingAssignmentRepository,
            examItemEvaluationRepository,
            examItemCriterionScoreRepository,
            examSessionRepository,
            upsertExamCandidateResultUseCase,
            examGradingAccessService,
            resolver
        );

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            new ExamItemResponse(responseId, sessionId, paperItemId, null, null, null, null, null)));
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of(
            criterion(fluencyId, "FLU", "Trôi chảy", new BigDecimal("0.60")),
            criterion(pronunciationId, "PRO", "Phát âm", new BigDecimal("0.40"))));
        when(rubricVersionRepository.findById(rubricVersionId)).thenReturn(Optional.of(rubricVersion()));
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            var evaluation = (ExamItemEvaluation) invocation.getArgument(0);
            if (evaluation.getId() == null) {
                evaluation.setId(UUID.randomUUID());
            }
            return evaluation;
        });
    }

    private RubricCriterion criterion(UUID id, String code, String name, BigDecimal weight) {
        return new RubricCriterion(id, rubricVersionId, null, code, name, null, null, weight,
            new BigDecimal("0.00"), new BigDecimal("9.00"), 1, true, null, null, null, null);
    }

    private RubricVersion rubricVersion() {
        var version = new RubricVersion();
        version.setTotalScoreMethod(RubricTotalScoreMethod.WEIGHTED_AVERAGE);
        version.setScoringScaleMin(BigDecimal.ZERO);
        version.setScoringScaleMax(BigDecimal.TEN);
        return version;
    }

    private GradingContext contextWith(
            GradingAssignmentStatus assignmentStatus, ExamCandidateResultStatus resultStatus, boolean flagged) {
        var assignment = new ExamGradingAssignment(
            assignmentId, candidateResultId, teacherId, assignmentStatus, OffsetDateTime.now(), null, null);

        var candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setSessionId(sessionId);
        candidateResult.setRubricVersionId(rubricVersionId);
        candidateResult.setStatus(resultStatus);

        var session = new ExamSession();
        session.setId(sessionId);
        session.setFlagged(flagged);
        session.setFlagReason(flagged ? "Phát hiện khuôn mặt khác trong khung hình" : null);

        return new GradingContext(assignment, candidateResult, session, schoolId, "IELTS Speaking Mock");
    }

    private GradingContext given(
            GradingAssignmentStatus assignmentStatus, ExamCandidateResultStatus resultStatus, boolean flagged) {
        var context = contextWith(assignmentStatus, resultStatus, flagged);
        when(examGradingAccessService.loadForGrading(assignmentId, null)).thenReturn(context);
        return context;
    }

    private void givenRecalculatedTo(ExamCandidateResultStatus status, String totalScore) {
        var recalculated = new ExamCandidateResult();
        recalculated.setStatus(status);
        recalculated.setTotalScore(new BigDecimal(totalScore));
        when(upsertExamCandidateResultUseCase.execute(sessionId)).thenReturn(recalculated);
    }

    private SubmitGradingCommand command(String fluency, String pronunciation) {
        return new SubmitGradingCommand(assignmentId, null, List.of(
            new SubmitGradingCommand.ItemGrade(paperItemId, List.of(
                new SubmitGradingCommand.CriterionScoreItem(fluencyId, new BigDecimal(fluency), "trôi chảy"),
                new SubmitGradingCommand.CriterionScoreItem(pronunciationId, new BigDecimal(pronunciation), null)
            ), "Cần luyện thêm phần mở bài")));
    }

    private ExamItemEvaluation captureSavedHumanEvaluation() {
        var captor = ArgumentCaptor.forClass(ExamItemEvaluation.class);
        verify(examItemEvaluationRepository, times(1)).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_write_finalized_human_evaluation_when_teacher_submits() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        var saved = captureSavedHumanEvaluation();
        assertThat(saved.getStatus()).isEqualTo(ExamItemEvaluationStatus.FINALIZED);
        assertThat(saved.getEngineType()).isEqualTo(ExamEvaluationEngineType.HUMAN);
        assertThat(saved.getGradedByModel()).isEqualTo("HUMAN");
        assertThat(saved.getReviewerId()).isEqualTo(teacherId);
        assertThat(saved.getResponseId()).isEqualTo(responseId);
        assertThat(saved.getFeedbackSummary()).isEqualTo("Cần luyện thêm phần mở bài");
    }

    @Test
    void should_weight_item_score_by_criterion_weight_not_plain_average() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        // (8.00*0.60 + 6.00*0.40) / 1.00 = 7.20 — trung bình cộng sẽ ra 7.00.
        assertThat(captureSavedHumanEvaluation().getItemScore()).isEqualByComparingTo("7.20");
    }

    @Test
    void should_not_require_human_review_on_the_human_evaluation_so_result_can_auto_release() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        assertThat(captureSavedHumanEvaluation().isRequiresHumanReview()).isFalse();
    }

    @Test
    void should_store_criterion_scores_against_the_new_evaluation() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        @SuppressWarnings("unchecked")
        var captor = (ArgumentCaptor<List<ExamItemCriterionScore>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(examItemCriterionScoreRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(criterionScore -> criterionScore.getFinalScore())
            .containsExactly(new BigDecimal("8.00"), new BigDecimal("6.00"));
    }

    @Test
    void should_supersede_previous_evaluations_of_graded_responses() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        var aiEvaluation = new ExamItemEvaluation();
        aiEvaluation.setStatus(ExamItemEvaluationStatus.AUTO_GRADED);
        when(examItemEvaluationRepository.findByResponseIdIn(List.of(responseId)))
            .thenReturn(List.of(aiEvaluation));
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        // Calculator chỉ nhìn bản mới nhất trong (AUTO_GRADED, FINALIZED). Bản AI
        // phải bị flip, nếu không hai bản cùng hợp lệ và điểm phụ thuộc thứ tự.
        assertThat(aiEvaluation.getStatus()).isEqualTo(ExamItemEvaluationStatus.SUPERSEDED);
        verify(examItemEvaluationRepository, times(2)).save(any());
    }

    @Test
    void should_release_result_and_complete_assignment_after_submit() {
        var context = given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        var response = useCase.execute(command("8.00", "6.00"));

        assertThat(response.candidateResultId()).isEqualTo(candidateResultId);
        assertThat(response.totalScore()).isEqualByComparingTo("7.20");
        assertThat(response.resultStatus()).isEqualTo("RELEASED");
        assertThat(context.assignment().getStatus()).isEqualTo(GradingAssignmentStatus.COMPLETED);
        assertThat(context.assignment().getCompletedAt()).isNotNull();
        verify(examGradingAssignmentRepository).save(context.assignment());
    }

    @Test
    void should_let_auto_resolve_decide_status_instead_of_forcing_released() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        // Chấm dở: còn phần khác chưa chấm nên auto-resolve giữ PENDING_REVIEW.
        givenRecalculatedTo(ExamCandidateResultStatus.PENDING_REVIEW, "5.00");

        var response = useCase.execute(command("8.00", "6.00"));

        // Overload MỘT tham số: trạng thái do resolveDefaultStatus quyết, không cưỡng bức.
        verify(upsertExamCandidateResultUseCase, times(1)).execute(sessionId);
        assertThat(response.resultStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void should_clear_flag_but_keep_flag_reason_when_grading_a_flagged_session() {
        var context = given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, true);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        assertThat(context.session().isFlagged()).isFalse();
        // flagReason ở lại để còn tra vì sao bài từng bị đánh dấu.
        assertThat(context.session().getFlagReason()).isNotBlank();
        verify(examSessionRepository).save(context.session());
    }

    @Test
    void should_not_touch_session_when_it_was_never_flagged() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        givenRecalculatedTo(ExamCandidateResultStatus.RELEASED, "7.20");

        useCase.execute(command("8.00", "6.00"));

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    void should_reject_when_teacher_is_not_the_assigned_one() {
        var context = given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examGradingAccessService).authorizeGrader(context, teacherId);

        assertThatThrownBy(() -> useCase.execute(command("8.00", "6.00")))
            .isInstanceOf(ForbiddenException.class);

        verify(examItemEvaluationRepository, never()).save(any());
        verify(upsertExamCandidateResultUseCase, never()).execute(any());
    }

    @Test
    void should_reject_grading_a_result_that_is_already_released() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.RELEASED, false);

        assertThatThrownBy(() -> useCase.execute(command("8.00", "6.00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không còn ở trạng thái chờ chấm");

        verify(examItemEvaluationRepository, never()).save(any());
        verify(upsertExamCandidateResultUseCase, never()).execute(any());
    }

    @Test
    void should_reject_submitting_twice() {
        given(GradingAssignmentStatus.COMPLETED, ExamCandidateResultStatus.PENDING_REVIEW, false);

        assertThatThrownBy(() -> useCase.execute(command("8.00", "6.00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã nộp");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_score_outside_criterion_range() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);

        assertThatThrownBy(() -> useCase.execute(command("9.50", "6.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0.00 - 9.00");

        // Validate hết rồi mới persist: không được để lại bản chấm nửa vời.
        verify(examItemEvaluationRepository, never()).save(any());
        verify(examSessionRepository, never()).save(any());
    }

    @Test
    void should_reject_when_a_required_criterion_is_missing() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        var partial = new SubmitGradingCommand(assignmentId, null, List.of(
            new SubmitGradingCommand.ItemGrade(paperItemId, List.of(
                new SubmitGradingCommand.CriterionScoreItem(fluencyId, new BigDecimal("8.00"), null)
            ), null)));

        assertThatThrownBy(() -> useCase.execute(partial))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phát âm");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_part_does_not_belong_to_the_exam() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        // Phủ đủ phần thật (để qua kiểm coverage) NHƯNG kèm một phần lạ không có response.
        var foreignPaperItemId = UUID.randomUUID();
        var withForeign = new SubmitGradingCommand(assignmentId, null, List.of(
            new SubmitGradingCommand.ItemGrade(paperItemId, List.of(
                new SubmitGradingCommand.CriterionScoreItem(fluencyId, new BigDecimal("8.00"), null),
                new SubmitGradingCommand.CriterionScoreItem(pronunciationId, new BigDecimal("6.00"), null)
            ), null),
            new SubmitGradingCommand.ItemGrade(foreignPaperItemId, List.of(
                new SubmitGradingCommand.CriterionScoreItem(fluencyId, new BigDecimal("8.00"), null),
                new SubmitGradingCommand.CriterionScoreItem(pronunciationId, new BigDecimal("6.00"), null)
            ), null)));

        assertThatThrownBy(() -> useCase.execute(withForeign))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không thuộc bài thi này");
    }

    @Test
    void should_reject_submitting_fewer_parts_than_the_exam_has() {
        // Bài có 2 phần nhưng chỉ nộp 1 → không được chốt COMPLETED để rồi khóa cứng
        // phần còn lại (bài COMPLETED không gỡ được). Ranh giới quan trọng nhất của fix này.
        var secondPaperItemId = UUID.randomUUID();
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            new ExamItemResponse(responseId, sessionId, paperItemId, null, null, null, null, null),
            new ExamItemResponse(UUID.randomUUID(), sessionId, secondPaperItemId, null, null, null, null, null)));

        assertThatThrownBy(() -> useCase.execute(command("8.00", "6.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phải chấm đủ");

        verify(examItemEvaluationRepository, never()).save(any());
        verify(examSessionRepository, never()).save(any());
        verify(upsertExamCandidateResultUseCase, never()).execute(any());
    }
}
