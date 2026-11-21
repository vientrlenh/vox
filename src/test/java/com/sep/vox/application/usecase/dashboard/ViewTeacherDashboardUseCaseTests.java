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
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ResolveAssessmentCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AssessmentCandidateAttempts;
import com.sep.vox.application.response.input.dashboard.SchoolClassScoreStatsResponse;
import com.sep.vox.application.response.input.dashboard.TeacherDashboardSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

class ViewTeacherDashboardUseCaseTests {

    private UserContextPort userContextPort;
    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private ResolveAssessmentCandidateAttemptsUseCase resolveAssessmentCandidateAttemptsUseCase;
    private ViewTeacherDashboardUseCase useCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        resolveAssessmentCandidateAttemptsUseCase = mock(ResolveAssessmentCandidateAttemptsUseCase.class);
        useCase = new ViewTeacherDashboardUseCase(
            userContextPort,
            assessmentRepository,
            assessmentMemberRepository,
            assessmentGradingAssignmentRepository,
            assessmentCandidateRepository,
            schoolClassRepository,
            schoolClassUserRepository,
            resolveAssessmentCandidateAttemptsUseCase
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacherId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(assessmentRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(AssessmentKind.CLASS_TEST), eq(null), eq(null), eq(1), eq(500)
        )).thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));
        when(assessmentMemberRepository.findByAssessmentIdIn(any())).thenReturn(List.of());
        when(assessmentGradingAssignmentRepository.countByTeacherIdAndStatus(any(), any())).thenReturn(0L);
        when(assessmentCandidateRepository.findByAssessmentIdIn(any())).thenReturn(List.of());
        when(schoolClassUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(schoolClassRepository.findAllById(any())).thenReturn(List.of());
        when(resolveAssessmentCandidateAttemptsUseCase.executeBatch(any())).thenReturn(Map.of());
    }

    private Assessment classTest(UUID assessmentId, AssessmentStatus status) {
        return classTest(assessmentId, status, "Bài kiểm tra");
    }

    private Assessment classTest(UUID assessmentId, AssessmentStatus status, String name) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(status);
        assessment.setName(name);
        return assessment;
    }

    private AssessmentCandidate candidate(UUID assessmentId, UUID studentId) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(assessmentId);
        candidate.setStudentId(studentId);
        return candidate;
    }

    private AssessmentMember membership(UUID assessmentId, UUID userId) {
        return AssessmentMember.builder()
                .id(UUID.randomUUID())
                .assessmentId(assessmentId)
                .userId(userId)
                .role(AssessmentMemberRole.AUTHOR)
                .grantedAt(Instant.now())
                .grantedBy(null)
                .build();
    }

    @Test
    void should_only_count_class_test_assessments_for_status_counts() {
        UUID draftAssessmentId = UUID.randomUUID();
        UUID publishedAssessmentId = UUID.randomUUID();
        when(assessmentRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(AssessmentKind.CLASS_TEST), eq(null), eq(null), eq(1), eq(500)
        )).thenReturn(new PageResult<>(
            List.of(classTest(draftAssessmentId, AssessmentStatus.DRAFT), classTest(publishedAssessmentId, AssessmentStatus.RESULTS_PUBLISHED)),
            0, 500, 2, 1
        ));
        when(assessmentMemberRepository.findByAssessmentIdIn(List.of(draftAssessmentId, publishedAssessmentId))).thenReturn(List.of(
            membership(draftAssessmentId, teacherId),
            membership(publishedAssessmentId, teacherId)
        ));

        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.assessmentStatusCounts().draft()).isEqualTo(1L);
        assertThat(result.assessmentStatusCounts().resultsPublished()).isEqualTo(1L);
        assertThat(result.assessmentStatusCounts().total()).isEqualTo(2L);
    }

    @Test
    void should_still_report_grading_stats() {
        when(assessmentGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.ASSIGNED))
            .thenReturn(3L);
        when(assessmentGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.COMPLETED))
            .thenReturn(7L);

        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.gradingStats().pending()).isEqualTo(3L);
        assertThat(result.gradingStats().completed()).isEqualTo(7L);
    }

    @Test
    void should_report_null_average_when_teacher_has_no_class_tests() {
        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isNull();
        assertThat(result.scoreStats().gradedCount()).isZero();
        assertThat(result.scoreStats().totalCandidates()).isZero();
    }

    @Test
    void should_compute_average_score_across_class_test_candidates() {
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = classTest(assessmentId, AssessmentStatus.RESULTS_PUBLISHED);

        when(assessmentRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(AssessmentKind.CLASS_TEST), eq(null), eq(null), eq(1), eq(500)
        )).thenReturn(new PageResult<>(List.of(assessment), 0, 500, 1, 1));
        when(assessmentMemberRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(membership(assessmentId, teacherId)));

        AssessmentCandidate candidateGraded = new AssessmentCandidate();
        candidateGraded.setId(UUID.randomUUID());
        candidateGraded.setAssessmentId(assessmentId);
        AssessmentCandidate candidateUngraded = new AssessmentCandidate();
        candidateUngraded.setId(UUID.randomUUID());
        candidateUngraded.setAssessmentId(assessmentId);
        when(assessmentCandidateRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(candidateGraded, candidateUngraded));

        when(resolveAssessmentCandidateAttemptsUseCase.executeBatch(
            List.of(candidateGraded.getId(), candidateUngraded.getId())
        )).thenReturn(Map.of(
            candidateGraded.getId(), new AssessmentCandidateAttempts(List.of(), null, BigDecimal.valueOf(8.50)),
            candidateUngraded.getId(), new AssessmentCandidateAttempts(List.of(), null, null)
        ));

        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isEqualByComparingTo(BigDecimal.valueOf(8.50));
        assertThat(result.scoreStats().gradedCount()).isEqualTo(1L);
        assertThat(result.scoreStats().totalCandidates()).isEqualTo(2L);
    }

    @Test
    void should_report_zero_graded_when_candidates_exist_but_none_are_graded() {
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = classTest(assessmentId, AssessmentStatus.RESULTS_PUBLISHED);

        when(assessmentRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(AssessmentKind.CLASS_TEST), eq(null), eq(null), eq(1), eq(500)
        )).thenReturn(new PageResult<>(List.of(assessment), 0, 500, 1, 1));
        when(assessmentMemberRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(membership(assessmentId, teacherId)));

        AssessmentCandidate candidateOne = new AssessmentCandidate();
        candidateOne.setId(UUID.randomUUID());
        candidateOne.setAssessmentId(assessmentId);
        AssessmentCandidate candidateTwo = new AssessmentCandidate();
        candidateTwo.setId(UUID.randomUUID());
        candidateTwo.setAssessmentId(assessmentId);
        when(assessmentCandidateRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(candidateOne, candidateTwo));

        when(resolveAssessmentCandidateAttemptsUseCase.executeBatch(
            List.of(candidateOne.getId(), candidateTwo.getId())
        )).thenReturn(Map.of(
            candidateOne.getId(), new AssessmentCandidateAttempts(List.of(), null, null),
            candidateTwo.getId(), new AssessmentCandidateAttempts(List.of(), null, null)
        ));

        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.scoreStats().averageScore()).isNull();
        assertThat(result.scoreStats().gradedCount()).isZero();
        assertThat(result.scoreStats().totalCandidates()).isEqualTo(2L);
    }

    @Test
    void should_report_per_class_test_score_breakdown_with_class_name_and_high_low() {
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = classTest(assessmentId, AssessmentStatus.RESULTS_PUBLISHED, "Kiểm tra 15 phút");
        UUID classId = UUID.randomUUID();
        UUID studentA = UUID.randomUUID();
        UUID studentB = UUID.randomUUID();
        UUID studentC = UUID.randomUUID();

        when(assessmentRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(AssessmentKind.CLASS_TEST), eq(null), eq(null), eq(1), eq(500)
        )).thenReturn(new PageResult<>(List.of(assessment), 0, 500, 1, 1));
        when(assessmentMemberRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(membership(assessmentId, teacherId)));

        AssessmentCandidate candidateA = candidate(assessmentId, studentA);
        AssessmentCandidate candidateB = candidate(assessmentId, studentB);
        AssessmentCandidate candidateC = candidate(assessmentId, studentC);
        when(assessmentCandidateRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(candidateA, candidateB, candidateC));

        when(schoolClassUserRepository.findByUserIdIn(List.of(studentA, studentB, studentC))).thenReturn(List.of(
            SchoolClassUser.builder()
                .userId(studentA)
                .schoolClassId(classId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(null)
                .build(),
            SchoolClassUser.builder()
                .userId(studentB)
                .schoolClassId(classId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(null)
                .build(),
            SchoolClassUser.builder()
                .userId(studentC)
                .schoolClassId(classId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(null)
                .build()
        ));
        SchoolClass schoolClass = SchoolClass.create(schoolId, UUID.randomUUID(), UUID.randomUUID(), "L10A1", "Lớp 10A1", null);
        schoolClass.setId(classId);
        when(schoolClassRepository.findAllById(List.of(classId))).thenReturn(List.of(schoolClass));

        when(resolveAssessmentCandidateAttemptsUseCase.executeBatch(
            List.of(candidateA.getId(), candidateB.getId(), candidateC.getId())
        )).thenReturn(Map.of(
            candidateA.getId(), new AssessmentCandidateAttempts(List.of(), null, BigDecimal.valueOf(9.0)),
            candidateB.getId(), new AssessmentCandidateAttempts(List.of(), null, BigDecimal.valueOf(5.0)),
            candidateC.getId(), new AssessmentCandidateAttempts(List.of(), null, null)
        ));

        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.classScoreStats()).hasSize(1);
        SchoolClassScoreStatsResponse stats = result.classScoreStats().get(0);
        assertThat(stats.assessmentName()).isEqualTo("Kiểm tra 15 phút");
        assertThat(stats.className()).isEqualTo("Lớp 10A1");
        assertThat(stats.averageScore()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        assertThat(stats.highestScore()).isEqualByComparingTo(BigDecimal.valueOf(9.0));
        assertThat(stats.lowestScore()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
        assertThat(stats.gradedCount()).isEqualTo(2L);
        assertThat(stats.totalCandidates()).isEqualTo(3L);
    }

    @Test
    void should_fall_back_to_unknown_class_name_when_class_membership_cannot_be_resolved() {
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = classTest(assessmentId, AssessmentStatus.RESULTS_PUBLISHED);

        when(assessmentRepository.findAccessible(
            eq(teacherId), eq(schoolId), eq(false), eq(false), eq(schoolId), eq(null),
            eq(AssessmentKind.CLASS_TEST), eq(null), eq(null), eq(1), eq(500)
        )).thenReturn(new PageResult<>(List.of(assessment), 0, 500, 1, 1));
        when(assessmentMemberRepository.findByAssessmentIdIn(List.of(assessmentId)))
            .thenReturn(List.of(membership(assessmentId, teacherId)));

        TeacherDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.classScoreStats()).hasSize(1);
        assertThat(result.classScoreStats().get(0).className()).isEqualTo("Không xác định");
        assertThat(result.classScoreStats().get(0).averageScore()).isNull();
        assertThat(result.classScoreStats().get(0).totalCandidates()).isZero();
    }
}
