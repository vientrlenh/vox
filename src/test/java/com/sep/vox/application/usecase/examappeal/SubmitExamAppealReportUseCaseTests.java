package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand.CriterionScoreItem;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.SubmitExamAppealReportUseCase;
import com.sep.vox.domain.model.exam.ExamAppealReviewer;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

public class SubmitExamAppealReportUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamAppealReviewerRepository examAppealReviewerRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private ExamAppealAccessService examAppealAccessService;
    private SubmitExamAppealReportUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID evaluationId = UUID.randomUUID();
    private final UUID criterion1 = UUID.randomUUID();
    private final UUID criterion2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examAppealReviewerRepository = mock(ExamAppealReviewerRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new SubmitExamAppealReportUseCase(
            examResultAppealRepository,
            examAppealReviewerRepository,
            examItemEvaluationRepository,
            examItemCriterionScoreRepository,
            rubricCriterionRepository,
            examAppealAccessService
        );

        when(examAppealAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId))
            .thenReturn(List.of(criterion("Fluency", criterion1), criterion("Grammar", criterion2)));
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            ExamItemEvaluation evaluation = invocation.getArgument(0);
            evaluation.setId(evaluationId);
            return evaluation;
        });
    }

    private RubricCriterion criterion(String name, UUID id) {
        var criterion = new RubricCriterion();
        criterion.setId(id);
        criterion.setName(name);
        criterion.setMinScore(new BigDecimal("0.00"));
        criterion.setMaxScore(new BigDecimal("9.00"));
        return criterion;
    }

    private AppealContext context(ExamAppealStatus status) {
        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(status);
        appeal.setResponseId(responseId);
        appeal.setPaperItemId(UUID.randomUUID());

        var candidateResult = new ExamCandidateResult();
        candidateResult.setRubricVersionId(rubricVersionId);
        candidateResult.setSessionId(UUID.randomUUID());

        return new AppealContext(appeal, candidateResult, new ExamSession(),
            UUID.randomUUID(), UUID.randomUUID(), "IELTS Speaking Mock");
    }

    private ExamAppealReviewer assignedReviewer() {
        var reviewer = new ExamAppealReviewer();
        reviewer.setId(UUID.randomUUID());
        reviewer.setAppealId(appealId);
        reviewer.setReviewerId(teacherId);
        reviewer.setStatus(ExamAppealReviewerStatus.ASSIGNED);
        reviewer.setAssignedAt(OffsetDateTime.now());
        return reviewer;
    }

    private SubmitExamAppealReportCommand command(BigDecimal score1, BigDecimal score2) {
        return new SubmitExamAppealReportCommand(
            appealId,
            List.of(new CriterionScoreItem(criterion1, score1, "ok"),
                new CriterionScoreItem(criterion2, score2, null)),
            "Nhận xét tổng"
        );
    }

    @Test
    void should_write_evaluation_as_under_review_so_calculator_cannot_see_it() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var reviewer = assignedReviewer();
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(reviewer));
        when(examAppealReviewerRepository.findByAppealId(appealId)).thenReturn(List.of(reviewer));

        useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00")));

        var captor = ArgumentCaptor.forClass(ExamItemEvaluation.class);
        verify(examItemEvaluationRepository).save(captor.capture());
        // Đây là bất biến chống rò rỉ: báo cáo chưa công bố phải vô hình với calculator.
        assertThat(captor.getValue().getStatus()).isEqualTo(ExamItemEvaluationStatus.UNDER_REVIEW);
        assertThat(captor.getValue().getReviewerId()).isEqualTo(teacherId);
    }

    @Test
    void should_average_criterion_scores_into_suggested_score() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var reviewer = assignedReviewer();
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(reviewer));
        when(examAppealReviewerRepository.findByAppealId(appealId)).thenReturn(List.of(reviewer));

        useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00")));

        assertThat(reviewer.getSuggestedScore()).isEqualByComparingTo("7.50");
        assertThat(reviewer.getStatus()).isEqualTo(ExamAppealReviewerStatus.SUBMITTED);
        assertThat(reviewer.getEvaluationId()).isEqualTo(evaluationId);
    }

    @Test
    void should_move_appeal_to_comparing_when_every_reviewer_submitted() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var me = assignedReviewer();
        var other = new ExamAppealReviewer();
        other.setStatus(ExamAppealReviewerStatus.SUBMITTED);
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(me));
        when(examAppealReviewerRepository.findByAppealId(appealId)).thenReturn(List.of(me, other));

        useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00")));

        var captor = ArgumentCaptor.forClass(ExamResultAppeal.class);
        verify(examResultAppealRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExamAppealStatus.COMPARING);
    }

    @Test
    void should_keep_appeal_in_grading_when_someone_has_not_submitted() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var me = assignedReviewer();
        var other = new ExamAppealReviewer();
        other.setStatus(ExamAppealReviewerStatus.ASSIGNED);
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(me));
        when(examAppealReviewerRepository.findByAppealId(appealId)).thenReturn(List.of(me, other));

        useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00")));

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_reviewer_not_assigned_to_appeal() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00"))))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("không được phân công");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_reviewer_already_submitted() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var reviewer = assignedReviewer();
        reviewer.setStatus(ExamAppealReviewerStatus.SUBMITTED);
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00"))))
            .isInstanceOf(IllegalStateException.class);

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_score_outside_criterion_range() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(assignedReviewer()));

        assertThatThrownBy(() -> useCase.execute(command(new BigDecimal("9.50"), new BigDecimal("7.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fluency");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_not_all_criteria_scored() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(assignedReviewer()));

        var partial = new SubmitExamAppealReportCommand(
            appealId, List.of(new CriterionScoreItem(criterion1, new BigDecimal("8.00"), null)), "note");

        assertThatThrownBy(() -> useCase.execute(partial))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đủ 2 tiêu chí");
    }

    @Test
    void should_reject_when_appeal_not_in_grading() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.COMPARING));

        assertThatThrownBy(() -> useCase.execute(command(new BigDecimal("8.00"), new BigDecimal("7.00"))))
            .isInstanceOf(IllegalStateException.class);
    }
}
