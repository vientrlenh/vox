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
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

class ViewTeacherDashboardUseCaseTests {

    private UserContextPort userContextPort;
    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
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
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        resolveExamCandidateAttemptsUseCase = mock(ResolveExamCandidateAttemptsUseCase.class);
        useCase = new ViewTeacherDashboardUseCase(
            userContextPort,
            examRepository,
            examMemberRepository,
            examGradingAssignmentRepository,
            examCandidateRepository,
            schoolClassRepository,
            schoolClassUserRepository,
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
        when(schoolClassUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(schoolClassRepository.findAllById(any())).thenReturn(List.of());
        when(resolveExamCandidateAttemptsUseCase.executeBatch(any())).thenReturn(Map.of());
    }

    private Exam classTest(UUID examId, ExamStatus status) {
        return classTest(examId, status, "Bài kiểm tra");
    }

    private Exam classTest(UUID examId, ExamStatus status, String name) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(status);
        exam.setName(name);
        return exam;
    }

    private ExamCandidate candidate(UUID examId, UUID studentId) {
        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setStudentId(studentId);
        return candidate;
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
        candidateGraded.setExamId(examId);
        var candidateUngraded = new ExamCandidate();
        candidateUngraded.setId(UUID.randomUUID());
        candidateUngraded.setExamId(examId);
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
        candidateOne.setExamId(examId);
        var candidateTwo = new ExamCandidate();
        candidateTwo.setId(UUID.randomUUID());
        candidateTwo.setExamId(examId);
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

    @Test
    void should_report_per_class_test_score_breakdown_with_class_name_and_high_low() {
        var examId = UUID.randomUUID();
        var exam = classTest(examId, ExamStatus.RESULTS_PUBLISHED, "Kiểm tra 15 phút");
        var classId = UUID.randomUUID();
        var studentA = UUID.randomUUID();
        var studentB = UUID.randomUUID();
        var studentC = UUID.randomUUID();

        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(exam), 0, 500, 1, 1));
        when(examMemberRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(membership(examId, teacherId)));

        var candidateA = candidate(examId, studentA);
        var candidateB = candidate(examId, studentB);
        var candidateC = candidate(examId, studentC);
        when(examCandidateRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(candidateA, candidateB, candidateC));

        when(schoolClassUserRepository.findByUserIdIn(List.of(studentA, studentB, studentC))).thenReturn(List.of(
            new SchoolClassUser(studentA, classId, true, Instant.now(), null, null),
            new SchoolClassUser(studentB, classId, true, Instant.now(), null, null),
            new SchoolClassUser(studentC, classId, true, Instant.now(), null, null)
        ));
        var schoolClass = SchoolClass.create(schoolId, UUID.randomUUID(), UUID.randomUUID(), "L10A1", "Lớp 10A1", null, teacherId, Instant.now());
        schoolClass.setId(classId);
        when(schoolClassRepository.findAllById(List.of(classId))).thenReturn(List.of(schoolClass));

        when(resolveExamCandidateAttemptsUseCase.executeBatch(
            List.of(candidateA.getId(), candidateB.getId(), candidateC.getId())
        )).thenReturn(Map.of(
            candidateA.getId(), new ExamCandidateAttempts(List.of(), null, BigDecimal.valueOf(9.0)),
            candidateB.getId(), new ExamCandidateAttempts(List.of(), null, BigDecimal.valueOf(5.0)),
            candidateC.getId(), new ExamCandidateAttempts(List.of(), null, null)
        ));

        var result = useCase.execute(null);

        assertThat(result.classScoreStats()).hasSize(1);
        var stats = result.classScoreStats().get(0);
        assertThat(stats.examName()).isEqualTo("Kiểm tra 15 phút");
        assertThat(stats.className()).isEqualTo("Lớp 10A1");
        assertThat(stats.averageScore()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        assertThat(stats.highestScore()).isEqualByComparingTo(BigDecimal.valueOf(9.0));
        assertThat(stats.lowestScore()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
        assertThat(stats.gradedCount()).isEqualTo(2L);
        assertThat(stats.totalCandidates()).isEqualTo(3L);
    }

    @Test
    void should_fall_back_to_unknown_class_name_when_class_membership_cannot_be_resolved() {
        var examId = UUID.randomUUID();
        var exam = classTest(examId, ExamStatus.RESULTS_PUBLISHED);

        when(examRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(ExamKind.CLASS_TEST), eq(null), eq(null), eq(0), eq(500)
        )).thenReturn(new PageResult<>(List.of(exam), 0, 500, 1, 1));
        when(examMemberRepository.findByExamIdIn(List.of(examId)))
            .thenReturn(List.of(membership(examId, teacherId)));

        var result = useCase.execute(null);

        assertThat(result.classScoreStats()).hasSize(1);
        assertThat(result.classScoreStats().get(0).className()).isEqualTo("Không xác định");
        assertThat(result.classScoreStats().get(0).averageScore()).isNull();
        assertThat(result.classScoreStats().get(0).totalCandidates()).isZero();
    }
}
