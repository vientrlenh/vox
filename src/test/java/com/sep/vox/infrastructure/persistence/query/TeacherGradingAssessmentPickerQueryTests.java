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

import com.sep.vox.application.query.dto.GradingAssessmentOptionInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
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
 * The assessment filter of the teacher's queue: the list that feeds the dropdown, and filtering by the
 * selected assessment.
 *
 * <p>The scope of the dropdown is derived FROM the set of assignments and not from the assessments table -- a teacher grading a
 * centralized assessment is not a member of the assessment so every ordinary read path over assessments is
 * closed to them. This test locks two things: the dropdown only shows the caller's own assessments, and
 * {@code assessmentId} only NARROWS and does not open any extra door.
 *
 * <p>It runs on real Postgres because this is JPQL built with {@code em.createQuery}: a wrong
 * field name only blows up at CALL time and not at compile time.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class TeacherGradingAssessmentPickerQueryTests {

    @Autowired
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID otherTeacherId = UUID.randomUUID();
    private final Instant now = Instant.now();
    private final Instant longAgo = Instant.now().minus(5, ChronoUnit.DAYS);

    private UUID schoolId;
    private UUID midtermAssessmentId;
    private UUID finalAssessmentId;
    private UUID otherTeacherAssessmentId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();

        // Kỳ thi giữa kỳ: 2 bài còn phải chấm + 1 bài đã chấm xong, giao gần đây nhất.
        midtermAssessmentId = seedAssessment("CENTRALIZED", "Kỳ thi giữa kỳ");
        seedAssignment(midtermAssessmentId, teacherId, "ASSIGNED", now);
        seedAssignment(midtermAssessmentId, teacherId, "ASSIGNED", now);
        seedAssignment(midtermAssessmentId, teacherId, "COMPLETED", now);

        // Kỳ thi cuối kỳ: chấm xong sạch, giao từ lâu.
        finalAssessmentId = seedAssessment("CENTRALIZED", "Kỳ thi cuối kỳ");
        seedAssignment(finalAssessmentId, teacherId, "COMPLETED", longAgo);

        // Bài kiểm tra trên lớp có màn riêng — không được lọt vào dropdown này.
        seedAssignment(seedAssessment("CLASS_TEST", "Kiểm tra 15 phút"), teacherId, "ASSIGNED", now);

        // Kỳ thi của giáo viên khác, cùng trường.
        otherTeacherAssessmentId = seedAssessment("CENTRALIZED", "Kỳ thi của người khác");
        seedAssignment(otherTeacherAssessmentId, otherTeacherId, "ASSIGNED", now);

        em.flush();
        em.clear();
    }

    @Test
    void should_list_only_the_assessments_the_teacher_has_assignments_in() {
        assertThat(pickerFor(teacherId))
            .extracting(option -> option.name())
            .containsExactlyInAnyOrder("Kỳ thi giữa kỳ", "Kỳ thi cuối kỳ");
    }

    /**
     * Chấm xong bài cuối cùng mà kỳ thi biến mất khỏi dropdown ngay dưới tay là lỗi:
     * giáo viên vẫn phải xem lại được những bài mình đã chấm.
     */
    @Test
    void should_still_list_an_assessment_whose_assignments_are_all_completed() {
        assertThat(pickerFor(teacherId))
            .filteredOn(option -> option.id().equals(finalAssessmentId))
            .singleElement()
            .satisfies(option -> {
                assertThat(option.taskCount()).isEqualTo(1);
                assertThat(option.openTaskCount()).isZero();
            });
    }

    @Test
    void should_keep_class_test_assessments_out_of_the_centralized_picker() {
        assertThat(pickerFor(teacherId))
            .extracting(option -> option.name())
            .doesNotContain("Kiểm tra 15 phút");
    }

    @Test
    void should_count_open_and_completed_tasks_per_assessment() {
        assertThat(pickerFor(teacherId))
            .filteredOn(option -> option.id().equals(midtermAssessmentId))
            .singleElement()
            .satisfies(option -> {
                assertThat(option.taskCount()).isEqualTo(3);
                assertThat(option.openTaskCount()).isEqualTo(2);
            });
    }

    /** Kỳ thi vừa được giao là kỳ thi đang chấm dở — thứ giáo viên tìm trước nhất. */
    @Test
    void should_order_assessments_by_the_most_recent_assignment_first() {
        assertThat(pickerFor(teacherId))
            .extracting(option -> option.id())
            .containsExactly(midtermAssessmentId, finalAssessmentId);
    }

    @Test
    void should_return_nothing_for_a_teacher_with_no_assignments() {
        assertThat(pickerFor(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_filter_the_teacher_queue_by_assessment_id() {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            teacherId, midtermAssessmentId, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.content()).hasSize(3)
            .allSatisfy(task -> assertThat(task.assessmentName()).isEqualTo("Kỳ thi giữa kỳ"));
        assertThat(page.totalElements()).isEqualTo(3);
    }

    /**
     * {@code assessmentId} is a filter, NOT a gate: the teacherId condition still stands next to it, so
     * guessing another person's assessment id opens nothing.
     *
     * <p>It also pins {@code totalElements}: a filter that narrows the content without narrowing
     * the count makes the FE build empty pages that cannot be clicked.
     */
    @Test
    void should_not_leak_another_teachers_tasks_when_filtering_by_assessment_id() {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            teacherId, otherTeacherAssessmentId, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    /**
     * Leaving assessmentId empty = every centralized assessment of the caller, as before the filter existed.
     */
    @Test
    void should_return_every_centralized_task_when_no_assessment_id_is_given() {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            teacherId, null, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(4);
    }

    private List<GradingAssessmentOptionInfo> pickerFor(UUID who) {
        return assessmentGradingQueryRepository.findAssessmentsWithTasksByTeacherId(who, "CENTRALIZED");
    }

    private UUID seedStudent() {
        return persist(UserJpaEntity.builder()
            .email("hs-" + UUID.randomUUID() + "@vox.test")
            .passwordHash("hash")
            .fullName("Trần Quang Thiên")
            .dateOfBirth(LocalDate.of(2008, 1, 1))
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build()).getId();
    }

    private UUID seedAssessment(String kind, String name) {
        return persist(AssessmentJpaEntity.builder()
                .blueprintId(null)
                .blueprintVersionId(null)
                .code("EX-" + UUID.randomUUID())
                .name(name)
                .description(null)
                .schoolId(schoolId)
                .languageId(UUID.randomUUID())
                .kind(kind)
                .deliveryMode("STUDENT_DEVICE")
                .status("CLOSED")
                .maxAttempt(1)
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

    /**
     * Each assignment needs its own submission, and each submission needs its own STUDENT: a
     * student can take an assessment only once ({@code uq_assessment_candidates_assessment_student}), and
     * {@code active_result_id} has a partial unique index, so a submission can have at most ONE
     * open assignment.
     */
    private void seedAssignment(UUID assessmentId, UUID assignedTo, String status, Instant assignedAt) {
        UUID paperId = UUID.randomUUID();
        UUID studentId = seedStudent();
        UUID candidateId = persist(AssessmentCandidateJpaEntity.builder()
                .id(null)
                .assessmentId(assessmentId)
                .studentId(studentId)
                .assignedPaperId(paperId)
                .scheduleId(null)
                .status("COMPLETED")
                .assignedAt(now)
                .updatedAt(now)
                .blockedAt(null)
                .assignedBy(null)
                .updatedBy(null)
                .build()).getId();
        UUID sessionId = persist(AssessmentSessionJpaEntity.builder()
                .id(null)
                .assessmentId(assessmentId)
                .candidateId(candidateId)
                .paperId(paperId)
                .startedAt(now)
                .submittedAt(now)
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

        boolean completed = "COMPLETED".equals(status);
        persist(AssessmentGradingAssignmentJpaEntity.builder()
                .id(null)
                .candidateResultId(candidateResultId)
                .teacherId(assignedTo)
                .roundType("INITIAL")
                .appealId(null)
                .status(status)
                .outcome(completed ? "UPHELD" : null)
                .scoreBefore(new BigDecimal("6.50"))
                .assignedAt(assignedAt)
                .assignedBy(assignedTo)
                .completedAt(completed ? assignedAt : null)
                .deadlineAt(null)
                .remindedAt(null)
                .reason(null)
                .activeResultId(// A closed row has a null activeResultId -- that is how the partial index works.
            completed ? null : candidateResultId)
                .build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
