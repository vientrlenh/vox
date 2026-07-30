package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand.CriterionScoreItem;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand.ItemReport;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.SubmitExamAppealReportUseCase;
import com.sep.vox.domain.model.exam.ExamAppealReviewer;
import com.sep.vox.domain.model.exam.ExamAppealReviewerItem;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.ExamAppealReviewerItemRepository;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

public class SubmitExamAppealReportUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamResultAppealItemRepository examResultAppealItemRepository;
    private ExamAppealReviewerRepository examAppealReviewerRepository;
    private ExamAppealReviewerItemRepository examAppealReviewerItemRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private ExamAppealAccessService examAppealAccessService;
    private SubmitExamAppealReportUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID otherResponseId = UUID.randomUUID();
    private final UUID appealItemId = UUID.randomUUID();
    private final UUID otherAppealItemId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID evaluationId = UUID.randomUUID();
    private final UUID otherEvaluationId = UUID.randomUUID();
    private final UUID criterion1 = UUID.randomUUID();
    private final UUID criterion2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examResultAppealItemRepository = mock(ExamResultAppealItemRepository.class);
        examAppealReviewerRepository = mock(ExamAppealReviewerRepository.class);
        examAppealReviewerItemRepository = mock(ExamAppealReviewerItemRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new SubmitExamAppealReportUseCase(
            examResultAppealRepository,
            examResultAppealItemRepository,
            examAppealReviewerRepository,
            examAppealReviewerItemRepository,
            examItemEvaluationRepository,
            examItemCriterionScoreRepository,
            rubricCriterionRepository,
            examAppealAccessService
        );

        when(examAppealAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(rubricCriterionRepository.findByRubricVersionId(rubricVersionId))
            .thenReturn(List.of(criterion("Fluency", criterion1), criterion("Grammar", criterion2)));
        when(examResultAppealItemRepository.findByAppealId(appealId))
            .thenReturn(List.of(appealItem(appealItemId, responseId)));

        // Id được cấp lần lượt, để test nhiều phần thi phân biệt được từng evaluation.
        var ids = new ArrayDeque<>(List.of(evaluationId, otherEvaluationId));
        when(examItemEvaluationRepository.save(any())).thenAnswer(invocation -> {
            ExamItemEvaluation evaluation = invocation.getArgument(0);
            evaluation.setId(ids.isEmpty() ? UUID.randomUUID() : ids.poll());
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

    private ExamResultAppealItem appealItem(UUID id, UUID responseId0) {
        return new ExamResultAppealItem(id, appealId, UUID.randomUUID(), responseId0, null);
    }

    private AppealContext context(ExamAppealStatus status) {
        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(status);

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
        reviewer.setAssignedAt(Instant.now());
        return reviewer;
    }

    private SubmitExamAppealReportCommand command(BigDecimal score1, BigDecimal score2) {
        return new SubmitExamAppealReportCommand(appealId, List.of(itemReport(appealItemId, score1, score2)));
    }

    private ItemReport itemReport(UUID itemId, BigDecimal score1, BigDecimal score2) {
        return new ItemReport(
            itemId,
            List.of(new CriterionScoreItem(criterion1, score1, "ok"),
                new CriterionScoreItem(criterion2, score2, null)),
            "Nhận xét tổng"
        );
    }

    @SuppressWarnings("unchecked")
    private List<ExamAppealReviewerItem> captureReviewerItems() {
        var captor = ArgumentCaptor.forClass(List.class);
        verify(examAppealReviewerItemRepository).saveAll(captor.capture());
        return captor.getValue();
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

        var items = captureReviewerItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getSuggestedScore()).isEqualByComparingTo("7.50");
        assertThat(items.get(0).getEvaluationId()).isEqualTo(evaluationId);
        assertThat(items.get(0).getAppealReviewerId()).isEqualTo(reviewer.getId());
        assertThat(reviewer.getStatus()).isEqualTo(ExamAppealReviewerStatus.SUBMITTED);
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

        var partial = new SubmitExamAppealReportCommand(appealId, List.of(new ItemReport(
            appealItemId, List.of(new CriterionScoreItem(criterion1, new BigDecimal("8.00"), null)), "note")));

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

    // ---- nhiều phần thi trong một đơn --------------------------------------

    private void givenTwoItemAppeal(ExamAppealReviewer reviewer) {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examResultAppealItemRepository.findByAppealId(appealId)).thenReturn(List.of(
            appealItem(appealItemId, responseId), appealItem(otherAppealItemId, otherResponseId)));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(reviewer));
        when(examAppealReviewerRepository.findByAppealId(appealId)).thenReturn(List.of(reviewer));
    }

    @Test
    void should_create_one_evaluation_per_appeal_item() {
        var reviewer = assignedReviewer();
        givenTwoItemAppeal(reviewer);

        useCase.execute(new SubmitExamAppealReportCommand(appealId, List.of(
            itemReport(appealItemId, new BigDecimal("8.00"), new BigDecimal("7.00")),
            itemReport(otherAppealItemId, new BigDecimal("6.00"), new BigDecimal("6.00")))));

        var captor = ArgumentCaptor.forClass(ExamItemEvaluation.class);
        verify(examItemEvaluationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(evaluation -> evaluation.getResponseId())
            .containsExactly(responseId, otherResponseId);
        assertThat(captor.getAllValues()).allSatisfy(evaluation ->
            assertThat(evaluation.getStatus()).isEqualTo(ExamItemEvaluationStatus.UNDER_REVIEW));
    }

    @Test
    void should_persist_per_item_note_and_suggested_score() {
        var reviewer = assignedReviewer();
        givenTwoItemAppeal(reviewer);

        useCase.execute(new SubmitExamAppealReportCommand(appealId, List.of(
            itemReport(appealItemId, new BigDecimal("8.00"), new BigDecimal("7.00")),
            itemReport(otherAppealItemId, new BigDecimal("6.00"), new BigDecimal("5.00")))));

        var items = captureReviewerItems();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(item -> item.getAppealItemId())
            .containsExactly(appealItemId, otherAppealItemId);
        assertThat(items.get(0).getSuggestedScore()).isEqualByComparingTo("7.50");
        assertThat(items.get(1).getSuggestedScore()).isEqualByComparingTo("5.50");
        assertThat(items).allSatisfy(item -> assertThat(item.getNote()).isEqualTo("Nhận xét tổng"));
    }

    @Test
    void should_mark_reviewer_submitted_only_after_all_items_graded() {
        var reviewer = assignedReviewer();
        givenTwoItemAppeal(reviewer);

        assertThatThrownBy(() -> useCase.execute(
            command(new BigDecimal("8.00"), new BigDecimal("7.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đủ 2 phần thi");

        assertThat(reviewer.getStatus()).isEqualTo(ExamAppealReviewerStatus.ASSIGNED);
        verify(examAppealReviewerRepository, never()).save(any());
    }

    @Test
    void should_reject_when_submitted_items_do_not_cover_all_appeal_items() {
        givenTwoItemAppeal(assignedReviewer());

        assertThatThrownBy(() -> useCase.execute(new SubmitExamAppealReportCommand(appealId, List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tất cả phần thi");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_when_item_does_not_belong_to_the_appeal() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacherId))
            .thenReturn(Optional.of(assignedReviewer()));

        assertThatThrownBy(() -> useCase.execute(new SubmitExamAppealReportCommand(appealId, List.of(
            itemReport(UUID.randomUUID(), new BigDecimal("8.00"), new BigDecimal("7.00"))))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không thuộc đơn phúc khảo");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_reject_duplicate_items_in_one_report() {
        givenTwoItemAppeal(assignedReviewer());

        assertThatThrownBy(() -> useCase.execute(new SubmitExamAppealReportCommand(appealId, List.of(
            itemReport(appealItemId, new BigDecimal("8.00"), new BigDecimal("7.00")),
            itemReport(appealItemId, new BigDecimal("6.00"), new BigDecimal("6.00"))))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trùng phần thi");

        verify(examItemEvaluationRepository, never()).save(any());
    }

    @Test
    void should_not_persist_anything_when_one_item_fails_validation() {
        givenTwoItemAppeal(assignedReviewer());

        assertThatThrownBy(() -> useCase.execute(new SubmitExamAppealReportCommand(appealId, List.of(
            itemReport(appealItemId, new BigDecimal("8.00"), new BigDecimal("7.00")),
            itemReport(otherAppealItemId, new BigDecimal("9.50"), new BigDecimal("6.00"))))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fluency");

        // Phần thi đầu hợp lệ nhưng vẫn không được ghi: validate hết rồi mới persist.
        verify(examItemEvaluationRepository, never()).save(any());
        verify(examAppealReviewerItemRepository, never()).saveAll(any());
    }
}
