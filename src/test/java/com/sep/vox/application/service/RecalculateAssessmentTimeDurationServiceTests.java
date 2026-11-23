package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

class RecalculateAssessmentTimeDurationServiceTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private QuestionRepository questionRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private RecalculateAssessmentTimeDurationService service;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        questionRepository = mock(QuestionRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        service = new RecalculateAssessmentTimeDurationService(
            assessmentRepository, assessmentPaperRepository, assessmentPaperItemRepository,
            questionRepository, mock(QuestionAssetRepository.class), assessmentScheduleRepository);
    }

    @Test
    void should_reject_when_recalculated_duration_exceeds_active_schedule() {
        givenAssessmentWithPaperDuration(AssessmentKind.CENTRALIZED, 3600);
        // Ca thi chỉ dài 30 phút, đề vừa tính ra 60 phút.
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(schedule(AssessmentScheduleStatus.PUBLISHED, start, start.plus(30, ChronoUnit.MINUTES))));

        assertThatThrownBy(() -> service.recalculate(assessmentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vượt quá thời lượng");
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void should_point_at_open_close_window_for_class_test() {
        givenAssessmentWithPaperDuration(AssessmentKind.CLASS_TEST, 3600);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(schedule(AssessmentScheduleStatus.PUBLISHED, start, start.plus(30, ChronoUnit.MINUTES))));

        assertThatThrownBy(() -> service.recalculate(assessmentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thời gian đóng bài");
    }

    @Test
    void should_ignore_completed_and_cancelled_schedules() {
        givenAssessmentWithPaperDuration(AssessmentKind.CENTRALIZED, 3600);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(AssessmentScheduleStatus.COMPLETED, start, start.plus(30, ChronoUnit.MINUTES)),
            schedule(AssessmentScheduleStatus.CANCELLED, start, start.plus(30, ChronoUnit.MINUTES)),
            schedule(AssessmentScheduleStatus.MOVED, start, start.plus(30, ChronoUnit.MINUTES))));

        service.recalculate(assessmentId);

        verify(assessmentRepository).save(any(Assessment.class));
    }

    @Test
    void should_pass_when_assessment_has_no_schedule() {
        Assessment assessment = givenAssessmentWithPaperDuration(AssessmentKind.CENTRALIZED, 3600);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());

        service.recalculate(assessmentId);

        assertThat(assessment.getAssessmentTimeDurationSecond()).isEqualTo(3600);
        verify(assessmentRepository).save(assessment);
    }

    @Test
    void should_allow_shortening_duration() {
        Assessment assessment = givenAssessmentWithPaperDuration(AssessmentKind.CENTRALIZED, 600);
        assessment.setAssessmentTimeDurationSecond(3600);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(schedule(AssessmentScheduleStatus.PUBLISHED, start, start.plus(30, ChronoUnit.MINUTES))));

        service.recalculate(assessmentId);

        assertThat(assessment.getAssessmentTimeDurationSecond()).isEqualTo(600);
        verify(assessmentRepository).save(assessment);
    }

    /** No variants => 0, not null: one state may be encoded in only one way. */
    @Test
    void should_write_zero_duration_when_assessment_has_no_paper() {
        Assessment assessment = assessment(AssessmentKind.CENTRALIZED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());

        service.recalculate(assessmentId);

        assertThat(assessment.getAssessmentTimeDurationSecond()).isEqualTo(0);
        verify(assessmentRepository).save(assessment);
        verify(assessmentScheduleRepository, never()).findByAssessmentId(any());
    }

    /**
     * The other branch of the same "cannot be computed yet" state: there are variants but no
     * question assigned yet.
     */
    @Test
    void should_write_zero_duration_when_papers_have_no_assigned_question() {
        Assessment assessment = assessment(AssessmentKind.CENTRALIZED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        AssessmentPaper paper = mock(AssessmentPaper.class);
        when(paper.getId()).thenReturn(paperId);
        when(paper.getTimeDurationSeconds()).thenReturn(0);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(paper));

        AssessmentPaperItem item = mock(AssessmentPaperItem.class);
        when(item.getPaperId()).thenReturn(paperId);
        when(item.getQuestionId()).thenReturn(null);
        when(assessmentPaperItemRepository.findByPaperIdIn(List.of(paperId))).thenReturn(List.of(item));
        when(questionRepository.findByIdIn(List.of())).thenReturn(List.of());
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());

        service.recalculate(assessmentId);

        assertThat(assessment.getAssessmentTimeDurationSecond()).isEqualTo(0);
        verify(assessmentRepository).save(assessment);
    }

    /**
     * Builds an assessment with exactly 1 variant and 1 item, whose question's total duration =
     * totalSeconds.
     */
    private Assessment givenAssessmentWithPaperDuration(AssessmentKind kind, int totalSeconds) {
        Assessment assessment = assessment(kind);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        AssessmentPaper paper = mock(AssessmentPaper.class);
        when(paper.getId()).thenReturn(paperId);
        when(paper.getTimeDurationSeconds()).thenReturn(totalSeconds);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(paper));

        // The items of every variant and the questions of every item are loaded with ONE batch
        // query, instead of one query per variant and then one query per question.
        AssessmentPaperItem item = mock(AssessmentPaperItem.class);
        when(item.getPaperId()).thenReturn(paperId);
        when(item.getQuestionId()).thenReturn(questionId);
        when(assessmentPaperItemRepository.findByPaperIdIn(List.of(paperId))).thenReturn(List.of(item));

        Question question = mock(Question.class);
        when(question.getId()).thenReturn(questionId);
        when(question.getPreparationTimeSeconds()).thenReturn(0);
        when(question.getMaxResponseSeconds()).thenReturn(totalSeconds);
        when(questionRepository.findByIdIn(List.of(questionId))).thenReturn(List.of(question));
        return assessment;
    }

    private Assessment assessment(AssessmentKind kind) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setKind(kind);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status, Instant from, Instant to) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(assessmentId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
