package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.dashboard.ViewTeacherDashboardUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ResolveExamCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ExamCandidateAttempts;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

class ViewTeacherDashboardUseCaseTests {

    private UserContextPort userContextPort;
    private ExamRepository examRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase;
    private ViewTeacherDashboardUseCase useCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        examRepository = mock(ExamRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        resolveExamCandidateAttemptsUseCase = mock(ResolveExamCandidateAttemptsUseCase.class);
        useCase = new ViewTeacherDashboardUseCase(
            userContextPort,
            examRepository,
            examGradingAssignmentRepository,
            examCandidateRepository,
            resolveExamCandidateAttemptsUseCase
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacherId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(examGradingAssignmentRepository.countByTeacherIdAndStatus(any(), any())).thenReturn(0L);
        when(examCandidateRepository.findByExamIdIn(any())).thenReturn(List.of());
        when(resolveExamCandidateAttemptsUseCase.executeBatch(any())).thenReturn(Map.of());
    }

    @Test
    void should_only_count_class_test_exams_for_status_counts() {
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), any(ExamStatus.class), eq(null), eq(0), eq(1)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));

        useCase.execute(null);

        verify(examRepository).findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(ExamStatus.DRAFT), eq(null), eq(0), eq(1)
        );
        verify(examRepository).findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(ExamStatus.RESULTS_PUBLISHED), eq(null), eq(0), eq(1)
        );
    }

    @Test
    void should_still_report_grading_stats() {
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), any(ExamStatus.class), eq(null), eq(0), eq(1)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.ASSIGNED))
            .thenReturn(3L);
        when(examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.COMPLETED))
            .thenReturn(7L);

        var result = useCase.execute(null);

        assertThat(result.gradingStats().pending()).isEqualTo(3L);
        assertThat(result.gradingStats().completed()).isEqualTo(7L);
    }

    @Test
    void should_report_null_average_when_teacher_has_no_class_tests() {
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), any(ExamStatus.class), eq(null), eq(0), eq(1)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));

        var result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isNull();
        assertThat(result.scoreStats().gradedCount()).isZero();
        assertThat(result.scoreStats().totalCandidates()).isZero();
    }

    @Test
    void should_compute_average_score_across_class_test_candidates() {
        var examId = UUID.randomUUID();
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.RESULTS_PUBLISHED);

        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), any(ExamStatus.class), eq(null), eq(0), eq(1)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(exam), 0, 500, 1, 1));

        var candidateGraded = new ExamCandidate();
        candidateGraded.setId(UUID.randomUUID());
        var candidateUngraded = new ExamCandidate();
        candidateUngraded.setId(UUID.randomUUID());
        when(examCandidateRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(candidateGraded, candidateUngraded));

        when(resolveExamCandidateAttemptsUseCase.executeBatch(
            List.of(candidateGraded.getId(), candidateUngraded.getId())
        )).thenReturn(Map.of(
            candidateGraded.getId(), new ExamCandidateAttempts(List.of(), null, BigDecimal.valueOf(8.50)),
            candidateUngraded.getId(), new ExamCandidateAttempts(List.of(), null, null)
        ));

        var result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isEqualByComparingTo(BigDecimal.valueOf(8.50));
        assertThat(result.scoreStats().gradedCount()).isEqualTo(1L);
        assertThat(result.scoreStats().totalCandidates()).isEqualTo(2L);
    }
}
