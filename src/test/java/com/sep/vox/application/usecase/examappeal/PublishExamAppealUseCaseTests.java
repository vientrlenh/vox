package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sep.vox.application.event.ExamAppealPublishedEvent;
import com.sep.vox.application.port.input.command.PublishExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.PublishExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

public class PublishExamAppealUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private RubricVersionRepository rubricVersionRepository;
    private UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private ExamAppealAccessService examAppealAccessService;
    private EventPublisherPort eventPublisherPort;
    private PublishExamAppealUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        upsertExamCandidateResultUseCase = mock(UpsertExamCandidateResultUseCase.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new PublishExamAppealUseCase(
            examResultAppealRepository,
            examItemEvaluationRepository,
            rubricVersionRepository,
            upsertExamCandidateResultUseCase,
            examAppealAccessService,
            eventPublisherPort
        );

        when(examAppealAccessService.requireActiveUserId()).thenReturn(adminId);
        var rubricVersion = new RubricVersion();
        rubricVersion.setScoringScaleMin(new BigDecimal("0.00"));
        rubricVersion.setScoringScaleMax(new BigDecimal("9.00"));
        when(rubricVersionRepository.findById(rubricVersionId)).thenReturn(Optional.of(rubricVersion));
    }

    private AppealContext contextWith(ExamAppealStatus status) {
        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(status);
        appeal.setResponseId(responseId);
        appeal.setPaperItemId(paperItemId);

        var candidateResult = new ExamCandidateResult();
        candidateResult.setSessionId(sessionId);
        candidateResult.setRubricVersionId(rubricVersionId);
        candidateResult.setTotalScore(new BigDecimal("6.00"));

        var session = new ExamSession();
        session.setId(sessionId);

        return new AppealContext(appeal, candidateResult, session, schoolId, studentId, "IELTS Speaking Mock");
    }

    @Test
    void should_publish_and_recalculate_total_from_part_score() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);
        when(examItemEvaluationRepository.findByResponseIdIn(List.of(responseId))).thenReturn(List.of());
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var recalculated = new ExamCandidateResult();
        recalculated.setTotalScore(new BigDecimal("7.25"));
        when(upsertExamCandidateResultUseCase.execute(sessionId, ExamCandidateResultStatus.FINAL))
            .thenReturn(recalculated);

        useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("8.00"), "Đã đối chiếu"));

        // score_after phải là TỔNG do calculator dẫn xuất, không phải partScore admin nhập.
        assertThat(context.appeal().getScoreAfter()).isEqualByComparingTo("7.25");
        assertThat(context.appeal().getStatus()).isEqualTo(ExamAppealStatus.PUBLISHED);
        assertThat(context.appeal().getResolvedBy()).isEqualTo(adminId);
        verify(upsertExamCandidateResultUseCase).execute(sessionId, ExamCandidateResultStatus.FINAL);
    }

    @Test
    void should_write_finalized_human_evaluation_with_part_score() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);
        when(examItemEvaluationRepository.findByResponseIdIn(List.of(responseId))).thenReturn(List.of());
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var recalculated = new ExamCandidateResult();
        recalculated.setTotalScore(new BigDecimal("7.25"));
        when(upsertExamCandidateResultUseCase.execute(any(), any())).thenReturn(recalculated);

        useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("8.00"), null));

        var captor = ArgumentCaptor.forClass(ExamItemEvaluation.class);
        verify(examItemEvaluationRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ExamItemEvaluationStatus.FINALIZED);
        assertThat(saved.getEngineType()).isEqualTo(ExamEvaluationEngineType.HUMAN);
        assertThat(saved.getItemScore()).isEqualByComparingTo("8.00");
        assertThat(saved.getResponseId()).isEqualTo(responseId);
    }

    @Test
    void should_supersede_previous_evaluations_when_publishing() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);

        var aiEvaluation = new ExamItemEvaluation();
        aiEvaluation.setStatus(ExamItemEvaluationStatus.AUTO_GRADED);
        var reviewerDraft = new ExamItemEvaluation();
        reviewerDraft.setStatus(ExamItemEvaluationStatus.UNDER_REVIEW);
        when(examItemEvaluationRepository.findByResponseIdIn(List.of(responseId)))
            .thenReturn(List.of(aiEvaluation, reviewerDraft));
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var recalculated = new ExamCandidateResult();
        recalculated.setTotalScore(new BigDecimal("7.25"));
        when(upsertExamCandidateResultUseCase.execute(any(), any())).thenReturn(recalculated);

        useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("8.00"), null));

        assertThat(aiEvaluation.getStatus()).isEqualTo(ExamItemEvaluationStatus.SUPERSEDED);
        assertThat(reviewerDraft.getStatus()).isEqualTo(ExamItemEvaluationStatus.SUPERSEDED);
        // 2 bản cũ + 1 bản FINALIZED mới
        verify(examItemEvaluationRepository, times(3)).save(any());
    }

    @Test
    void should_publish_event_with_scores_before_and_after() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);
        when(examItemEvaluationRepository.findByResponseIdIn(anyList())).thenReturn(List.of());
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var recalculated = new ExamCandidateResult();
        recalculated.setTotalScore(new BigDecimal("7.25"));
        when(upsertExamCandidateResultUseCase.execute(any(), any())).thenReturn(recalculated);

        useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("8.00"), null));

        var captor = ArgumentCaptor.forClass(ExamAppealPublishedEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        var event = captor.getValue();
        assertThat(event.studentId()).isEqualTo(studentId);
        assertThat(event.scoreBefore()).isEqualByComparingTo("6.00");
        assertThat(event.scoreAfter()).isEqualByComparingTo("7.25");
    }

    @Test
    void should_reject_when_appeal_not_in_comparing() {
        var context = contextWith(ExamAppealStatus.GRADING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);

        assertThatThrownBy(() ->
            useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("8.00"), null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("tất cả giám khảo đã nộp");

        verify(examItemEvaluationRepository, never()).save(any());
        verify(upsertExamCandidateResultUseCase, never()).execute(any(), any());
    }

    @Test
    void should_reject_when_part_score_outside_rubric_scale() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);

        assertThatThrownBy(() ->
            useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("9.50"), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0.00 - 9.00");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_caller_is_not_school_admin_of_the_exam() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);
        org.mockito.Mockito.doThrow(new com.sep.vox.application.exception.ForbiddenException("BẢO MẬT"))
            .when(examAppealAccessService).authorizeSchoolAdmin(eq(context), eq(adminId));

        assertThatThrownBy(() ->
            useCase.execute(new PublishExamAppealCommand(appealId, new BigDecimal("8.00"), null)))
            .isInstanceOf(com.sep.vox.application.exception.ForbiddenException.class);

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_part_score_missing() {
        var context = contextWith(ExamAppealStatus.COMPARING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);

        assertThatThrownBy(() -> useCase.execute(new PublishExamAppealCommand(appealId, null, null)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
