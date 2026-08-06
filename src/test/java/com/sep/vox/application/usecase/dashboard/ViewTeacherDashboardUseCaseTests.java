package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

class ViewTeacherDashboardUseCaseTests {

    private UserContextPort userContextPort;
    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
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
        examMemberRepository = mock(ExamMemberRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        resolveExamCandidateAttemptsUseCase = mock(ResolveExamCandidateAttemptsUseCase.class);
        useCase = new ViewTeacherDashboardUseCase(
            userContextPort,
            examRepository,
            examMemberRepository,
            examGradingAssignmentRepository,
            examCandidateRepository,
            resolveExamCandidateAttemptsUseCase
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacherId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));
        when(examMemberRepository.findByExamIdIn(any())).thenReturn(List.of());
        when(examGradingAssignmentRepository.countByTeacherIdAndStatus(any(), any())).thenReturn(0L);
        when(examCandidateRepository.findByExamIdIn(any())).thenReturn(List.of());
        when(resolveExamCandidateAttemptsUseCase.executeBatch(any())).thenReturn(Map.of());
    }

    private Exam classTest(UUID examId, ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(status);
        return exam;
    }

    private ExamMember membership(UUID examId, UUID userId) {
        return new ExamMember(UUID.randomUUID(), examId, userId, ExamMemberRole.AUTHOR, Instant.now(), null);
    }

    @Test
    void should_only_count_class_test_exams_for_status_counts() {
        var draftExamId = UUID.randomUUID();
        var publishedExamId = UUID.randomUUID();
        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(
            List.of(classTest(draftExamId, ExamStatus.DRAFT), classTest(publishedExamId, ExamStatus.RESULTS_PUBLISHED)),
            0, 500, 2, 1
        ));
        when(examMemberRepository.findByExamIdIn(List.of(draftExamId, publishedExamId))).thenReturn(List.of(
            membership(draftExamId, teacherId),
            membership(publishedExamId, teacherId)
        ));

        var result = useCase.execute(null);

        assertThat(result.examStatusCounts().draft()).isEqualTo(1L);
        assertThat(result.examStatusCounts().resultsPublished()).isEqualTo(1L);
        assertThat(result.examStatusCounts().total()).isEqualTo(2L);
    }

    @Test
    void should_still_report_grading_stats() {
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
        var result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isNull();
        assertThat(result.scoreStats().gradedCount()).isZero();
        assertThat(result.scoreStats().totalCandidates()).isZero();
    }

    @Test
    void should_compute_average_score_across_class_test_candidates() {
        var examId = UUID.randomUUID();
        var exam = classTest(examId, ExamStatus.RESULTS_PUBLISHED);

        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(exam), 0, 500, 1, 1));
        when(examMemberRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(membership(examId, teacherId)));

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

    @Test
    void should_report_zero_graded_when_candidates_exist_but_none_are_graded() {
        var examId = UUID.randomUUID();
        var exam = classTest(examId, ExamStatus.RESULTS_PUBLISHED);

        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(exam), 0, 500, 1, 1));
        when(examMemberRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(membership(examId, teacherId)));

        var candidateOne = new ExamCandidate();
        candidateOne.setId(UUID.randomUUID());
        var candidateTwo = new ExamCandidate();
        candidateTwo.setId(UUID.randomUUID());
        when(examCandidateRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(candidateOne, candidateTwo));

        when(resolveExamCandidateAttemptsUseCase.executeBatch(
            List.of(candidateOne.getId(), candidateTwo.getId())
        )).thenReturn(Map.of(
            candidateOne.getId(), new ExamCandidateAttempts(List.of(), null, null),
            candidateTwo.getId(), new ExamCandidateAttempts(List.of(), null, null)
        ));

        var result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isNull();
        assertThat(result.scoreStats().gradedCount()).isZero();
        assertThat(result.scoreStats().totalCandidates()).isEqualTo(2L);
    }
}
