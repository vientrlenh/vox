package com.sep.vox.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ExamHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Đa số bài kiểm tra trên lớp đóng bằng đường này chứ không phải người bấm tay, nên side-effect của
 * nó phải khớp {@code UpdateExamStatusUseCase.CLOSE} — kể cả phần kéo theo ca thi.
 */
class ExamStatusAutoTransitionJobTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamStatusAutoTransitionJob job;

    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);

        when(examRepository.findByStatusAndOpenAtBefore(any(), any())).thenReturn(List.of());
        when(examRepository.findByStatusAndCloseAtBefore(any(), any())).thenReturn(List.of());

        job = new ExamStatusAutoTransitionJob(
            examRepository,
            mock(ExamPaperRepository.class),
            mock(ExamQuestionSecureLockService.class),
            mock(ZeroScoreExamResultService.class),
            mock(ClassTestGradingAssignmentService.class),
            mock(ExamHumanGradingNotificationService.class),
            new ExamScheduleClosureService(examScheduleRepository, examSessionRepository));
    }

    @Test
    void should_close_schedules_when_auto_closing_a_class_test() {
        var exam = classTest();
        var ended = schedule(ExamScheduleStatus.PUBLISHED,
            Instant.now().minus(3, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));
        when(examRepository.findByStatusAndCloseAtBefore(eq(ExamStatus.IN_PROGRESS), any())).thenReturn(List.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ended));

        job.run();

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CLOSED);
        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        verify(examScheduleRepository).save(ended);
    }

    /**
     * Nhánh phòng thủ: dữ liệu lệch khiến closeAt tới trước khi ca thi kết thúc. Job bỏ qua lượt này
     * chứ không cắt ngang buổi thi — tick sau (60s) thử lại.
     */
    @Test
    void should_defer_closing_when_a_student_is_still_working() {
        var exam = classTest();
        var ongoing = schedule(ExamScheduleStatus.PUBLISHED,
            Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS));
        when(examRepository.findByStatusAndCloseAtBefore(eq(ExamStatus.IN_PROGRESS), any())).thenReturn(List.of(exam));
        when(examScheduleRepository.findByExamIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(1L);

        job.run();

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.IN_PROGRESS);
        verify(examRepository, never()).save(any(Exam.class));
        verify(examScheduleRepository, never()).save(any());
    }

    /** Kỳ thi tập trung không tự đóng — job này cố ý chỉ nhận bài trên lớp. */
    @Test
    void should_skip_centralized_exam() {
        var exam = classTest();
        exam.setKind(ExamKind.CENTRALIZED);
        when(examRepository.findByStatusAndCloseAtBefore(eq(ExamStatus.IN_PROGRESS), any())).thenReturn(List.of(exam));

        job.run();

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.IN_PROGRESS);
        verify(examScheduleRepository, never()).save(any());
    }

    private Exam classTest() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.IN_PROGRESS);
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
