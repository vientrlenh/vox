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

import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.QuestionRepository;

class RecalculateExamTimeDurationServiceTests {

    private ExamRepository examRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private QuestionRepository questionRepository;
    private ExamScheduleRepository examScheduleRepository;
    private RecalculateExamTimeDurationService service;

    private final UUID examId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final Instant start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        questionRepository = mock(QuestionRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        service = new RecalculateExamTimeDurationService(
            examRepository, examPaperRepository, examPaperItemRepository,
            questionRepository, examScheduleRepository);
    }

    @Test
    void should_reject_when_recalculated_duration_exceeds_active_schedule() {
        givenExamWithPaperDuration(ExamKind.CENTRALIZED, 3600);
        // Ca thi chỉ dài 30 phút, đề vừa tính ra 60 phút.
        when(examScheduleRepository.findByExamId(examId))
            .thenReturn(List.of(schedule(ExamScheduleStatus.PUBLISHED, start, start.plus(30, ChronoUnit.MINUTES))));

        assertThatThrownBy(() -> service.recalculate(examId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("vượt quá thời lượng");
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    void should_point_at_open_close_window_for_class_test() {
        givenExamWithPaperDuration(ExamKind.CLASS_TEST, 3600);
        when(examScheduleRepository.findByExamId(examId))
            .thenReturn(List.of(schedule(ExamScheduleStatus.PUBLISHED, start, start.plus(30, ChronoUnit.MINUTES))));

        assertThatThrownBy(() -> service.recalculate(examId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("thời gian đóng bài");
    }

    @Test
    void should_ignore_completed_and_cancelled_schedules() {
        givenExamWithPaperDuration(ExamKind.CENTRALIZED, 3600);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(ExamScheduleStatus.COMPLETED, start, start.plus(30, ChronoUnit.MINUTES)),
            schedule(ExamScheduleStatus.CANCELLED, start, start.plus(30, ChronoUnit.MINUTES)),
            schedule(ExamScheduleStatus.MOVED, start, start.plus(30, ChronoUnit.MINUTES))));

        service.recalculate(examId);

        verify(examRepository).save(any(Exam.class));
    }

    @Test
    void should_pass_when_exam_has_no_schedule() {
        var exam = givenExamWithPaperDuration(ExamKind.CENTRALIZED, 3600);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of());

        service.recalculate(examId);

        assertThat(exam.getExamTimeDurationSecond()).isEqualTo(3600);
        verify(examRepository).save(exam);
    }

    @Test
    void should_allow_shortening_duration() {
        var exam = givenExamWithPaperDuration(ExamKind.CENTRALIZED, 600);
        exam.setExamTimeDurationSecond(3600);
        when(examScheduleRepository.findByExamId(examId))
            .thenReturn(List.of(schedule(ExamScheduleStatus.PUBLISHED, start, start.plus(30, ChronoUnit.MINUTES))));

        service.recalculate(examId);

        assertThat(exam.getExamTimeDurationSecond()).isEqualTo(600);
        verify(examRepository).save(exam);
    }

    /** Không mã đề nào ⇒ 0, không phải null: một trạng thái chỉ được có một cách mã hoá. */
    @Test
    void should_write_zero_duration_when_exam_has_no_paper() {
        var exam = exam(ExamKind.CENTRALIZED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of());

        service.recalculate(examId);

        assertThat(exam.getExamTimeDurationSecond()).isEqualTo(0);
        verify(examRepository).save(exam);
        verify(examScheduleRepository, never()).findByExamId(any());
    }

    /** Nhánh còn lại của cùng trạng thái "chưa tính được": có mã đề nhưng chưa gán câu hỏi nào. */
    @Test
    void should_write_zero_duration_when_papers_have_no_assigned_question() {
        var exam = exam(ExamKind.CENTRALIZED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        var paper = mock(ExamPaper.class);
        when(paper.getId()).thenReturn(paperId);
        when(paper.getTimeDurationSeconds()).thenReturn(0);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(paper));

        var item = mock(ExamPaperItem.class);
        when(item.getPaperId()).thenReturn(paperId);
        when(item.getQuestionId()).thenReturn(null);
        when(examPaperItemRepository.findByPaperIdIn(List.of(paperId))).thenReturn(List.of(item));
        when(questionRepository.findByIdIn(List.of())).thenReturn(List.of());
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of());

        service.recalculate(examId);

        assertThat(exam.getExamTimeDurationSecond()).isEqualTo(0);
        verify(examRepository).save(exam);
    }

    /** Dựng exam có đúng 1 mã đề, 1 item, câu hỏi tổng thời lượng = totalSeconds. */
    private Exam givenExamWithPaperDuration(ExamKind kind, int totalSeconds) {
        var exam = exam(kind);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        var paper = mock(ExamPaper.class);
        when(paper.getId()).thenReturn(paperId);
        when(paper.getTimeDurationSeconds()).thenReturn(totalSeconds);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(paper));

        // Item của mọi mã đề và câu hỏi của mọi item đều được nạp bằng MỘT query batch, thay vì
        // một query cho mỗi mã đề rồi một query cho mỗi câu hỏi.
        var item = mock(ExamPaperItem.class);
        when(item.getPaperId()).thenReturn(paperId);
        when(item.getQuestionId()).thenReturn(questionId);
        when(examPaperItemRepository.findByPaperIdIn(List.of(paperId))).thenReturn(List.of(item));

        var question = mock(Question.class);
        when(question.getId()).thenReturn(questionId);
        when(question.getPreparationTimeSeconds()).thenReturn(0);
        when(question.getMaxResponseSeconds()).thenReturn(totalSeconds);
        when(questionRepository.findByIdIn(List.of(questionId))).thenReturn(List.of(question));
        return exam;
    }

    private Exam exam(ExamKind kind) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setKind(kind);
        return exam;
    }

    private ExamSchedule schedule(ExamScheduleStatus status, Instant from, Instant to) {
        var schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(examId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
