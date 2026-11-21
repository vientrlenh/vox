package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.user.UserJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The number of attempts on the grading screen.
 *
 * <p>A student who retakes produces several {@code assessment_sessions}, each session with its own
 * {@code assessment_candidate_results} -- i.e. several ROWS with the same name on the grading screen. Without
 * {@code attemptNo}/{@code attemptCount} the grader sees two identical rows and
 * does not know which attempt they are grading.
 *
 * <p>The DB has no attempt-number column: the order is derived from {@code startedAt}. The "attempt 2" case is the most
 * important one here -- it locks that the attempt under consideration is matched to its own session and not
 * simply the first session.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class GradingAttemptQueryTests {

    @Autowired
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private static final String RETAKING_STUDENT = "Trần Quang Thiên";
    private static final String SINGLE_ATTEMPT_STUDENT = "Lê Văn Việt";

    private UUID schoolId;
    private UUID assessmentId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();
        UUID retakingStudentId = persist(user(RETAKING_STUDENT)).getId();
        UUID singleAttemptStudentId = persist(user(SINGLE_ATTEMPT_STUDENT)).getId();

        assessmentId = seedAssessment("Kiểm tra 15 phút");

        UUID retakingCandidateId = seedCandidate(assessmentId, retakingStudentId);
        // The two attempts are SEPARATED in time: the attempt order is derived from startedAt, and
        // with the same instant there is no order left to derive.
        seedAttempt(assessmentId, retakingCandidateId, now.minus(2, ChronoUnit.HOURS));
        seedAttempt(assessmentId, retakingCandidateId, now.minus(1, ChronoUnit.HOURS));

        UUID singleCandidateId = seedCandidate(assessmentId, singleAttemptStudentId);
        seedAttempt(assessmentId, singleCandidateId, now.minus(2, ChronoUnit.HOURS));

        em.flush();
        em.clear();
    }

    @Test
    void should_number_both_attempts_of_the_same_student_on_the_board() {
        List<GradingAssignmentRowInfo> rows = boardRowsOf(RETAKING_STUDENT);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.attemptNo()).containsExactlyInAnyOrder(1, 2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.attemptCount()).isEqualTo(2);
            assertThat(row.sessionId()).isNotNull();
        });
        // Each row must point back to its own session -- the same sessionId means one session was
        // picked for both attempts by mistake.
        assertThat(rows.get(0).sessionId()).isNotEqualTo(rows.get(1).sessionId());
    }

    @Test
    void should_report_a_single_attempt_when_the_student_took_the_assessment_once() {
        assertThat(boardRowsOf(SINGLE_ATTEMPT_STUDENT)).singleElement().satisfies(row -> {
            assertThat(row.attemptNo()).isEqualTo(1);
            assertThat(row.attemptCount()).isEqualTo(1);
        });
    }

    private List<GradingAssignmentRowInfo> boardRowsOf(String studentName) {
        return assessmentGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId, assessmentId, null, null, null, null, null, false, false, null, null,
            "CLASS_TEST"), 1, 20).content().stream()
            .filter(row -> studentName.equals(row.studentName()))
            .toList();
    }

    @Test
    void should_number_attempts_in_the_teacher_queue() {
        List<GradingTaskInfo> tasks = assessmentGradingQueryRepository
            .findTasksByTeacherIdAndAssessmentId(teacherId, assessmentId, null, null, null, 1, 20).content();

        assertThat(tasks).hasSize(3);
        assertThat(tasks).filteredOn(task -> task.attemptCount() == 2)
            .extracting(task -> task.attemptNo())
            .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void should_number_the_attempt_on_the_grading_detail() {
        GradingTaskInfo secondAttempt = assessmentGradingQueryRepository
            .findTasksByTeacherIdAndAssessmentId(teacherId, assessmentId, null, null, null, 1, 20).content().stream()
            .filter(task -> task.attemptNo() == 2)
            .findFirst()
            .orElseThrow();

        GradingTaskDetailInfo detail = assessmentGradingQueryRepository
            .findTaskDetail(secondAttempt.assignmentId(), teacherId).orElseThrow();

        assertThat(detail.attemptNo()).isEqualTo(2);
        assertThat(detail.attemptCount()).isEqualTo(2);
        assertThat(detail.sessionId()).isEqualTo(secondAttempt.sessionId());
    }

    private UserJpaEntity user(String fullName) {
        return UserJpaEntity.builder()
            .email("hs-" + UUID.randomUUID() + "@vox.test")
            .passwordHash("hash")
            .fullName(fullName)
            .dateOfBirth(LocalDate.of(2008, 1, 1))
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private UUID seedAssessment(String name) {
        return persist(AssessmentJpaEntity.builder()
                .blueprintId(null)
                .blueprintVersionId(null)
                .code("EX-" + UUID.randomUUID())
                .name(name)
                .description(null)
                .schoolId(schoolId)
                .languageId(UUID.randomUUID())
                .kind("CLASS_TEST")
                .deliveryMode("STUDENT_DEVICE")
                .status("CLOSED")
                .maxAttempt(2)
                .assessmentTimeDurationSecond(900)
                .resultDecisionMethod("HIGHEST")
                .requiredStreamType(null)
                .streamTypePermission(null)
                .openAt(null)
                .closeAt(null)
                .assessmentPolicyId(UUID.randomUUID())
                .requiresOtp(false)
                .build()).getId();
    }

    private UUID seedCandidate(UUID assessmentId, UUID studentId) {
        return persist(AssessmentCandidateJpaEntity.builder()
                .id(null)
                .assessmentId(assessmentId)
                .studentId(studentId)
                .assignedPaperId(UUID.randomUUID())
                .scheduleId(null)
                .status("COMPLETED")
                .assignedAt(now)
                .updatedAt(now)
                .blockedAt(null)
                .assignedBy(null)
                .updatedBy(null)
                .build()).getId();
    }

    private void seedAttempt(UUID assessmentId, UUID candidateId, Instant startedAt) {
        UUID sessionId = persist(AssessmentSessionJpaEntity.builder()
                .id(null)
                .assessmentId(assessmentId)
                .candidateId(candidateId)
                .paperId(UUID.randomUUID())
                .startedAt(startedAt)
                .submittedAt(startedAt)
                .status("GRADED")
                .flagged(false)
                .flagReason(null)
                .build()).getId();
        UUID candidateResultId = persist(AssessmentCandidateResultJpaEntity.builder()
                .assessmentId(assessmentId)
                .candidateId(candidateId)
                .sessionId(sessionId)
                .assessmentPolicyId(UUID.randomUUID())
                .policyVersion(1)
                .rubricVersionId(UUID.randomUUID())
                .frameworkVersionId(UUID.randomUUID())
                .targetFrameworkBandId(UUID.randomUUID())
                .rubricResultBandId(null)
                .totalScore(new BigDecimal("6.50"))
                .status("PENDING_REVIEW")
                .releasedAt(null)
                .finalizedAt(null)
                .build()).getId();
        persist(AssessmentGradingAssignmentJpaEntity.builder()
                .id(null)
                .candidateResultId(candidateResultId)
                .teacherId(teacherId)
                .roundType("INITIAL")
                .appealId(null)
                .status("ASSIGNED")
                .outcome(null)
                .scoreBefore(new BigDecimal("6.50"))
                .assignedAt(now)
                .assignedBy(teacherId)
                .completedAt(null)
                .deadlineAt(null)
                .remindedAt(null)
                .reason(null)
                .activeResultId(candidateResultId)
                .build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
