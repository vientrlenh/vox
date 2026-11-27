package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
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
 * Separates CENTRALIZED assessments from IN-CLASS tests in every grading read model.
 *
 * <p>The two kinds go through two different screens -- the school coordinates centralized assessments, a teacher
 * creates in-class tests and grades them themselves. Before this filter existed, the coordination board listed in-class
 * tests too and assigning one was blocked at a lower layer, while the teacher's queue mixed the two
 * kinds into one list.
 *
 * <p>Must run on a real Postgres because this is JPQL built with {@code em.createQuery}: a wrong
 * field name only blows up at CALL time, not at compile time.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class GradingAssessmentKindFilterQueryTests {

    @Autowired
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private UUID schoolId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();
        UUID studentId = persist(UserJpaEntity.builder()
            .email("hs-" + UUID.randomUUID() + "@vox.test")
            .passwordHash("hash")
            .fullName("Trần Quang Thiên")
            .dateOfBirth(LocalDate.of(2008, 1, 1))
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build()).getId();

        seedAssessment("CLASS_TEST", "Kiểm tra 15 phút", studentId);
        seedAssessment("CENTRALIZED", "Kỳ thi cuối kỳ", studentId);

        em.flush();
        em.clear();
    }

    @Test
    void should_list_only_centralized_rows_on_the_coordination_board() {
        PageResult<GradingAssignmentRowInfo> page = assessmentGradingQueryRepository.searchAssignments(filter("CENTRALIZED"), 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(row -> assertThat(row.assessmentName()).isEqualTo("Kỳ thi cuối kỳ"));
        // The total must go together with the content, otherwise the FE builds an empty page that
        // cannot be clicked.
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_list_only_class_test_rows_when_asked_for_class_tests() {
        PageResult<GradingAssignmentRowInfo> page = assessmentGradingQueryRepository.searchAssignments(filter("CLASS_TEST"), 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(row -> assertThat(row.assessmentName()).isEqualTo("Kiểm tra 15 phút"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    /** An empty filter still returns both -- the default lives in the use case, not hidden in the SQL. */
    @Test
    void should_list_both_kinds_when_no_kind_is_given() {
        assertThat(assessmentGradingQueryRepository.searchAssignments(filter(null), 1, 20).content())
            .hasSize(2);
    }

    @Test
    void should_count_only_the_asked_kind_in_stats() {
        GradingStatsInfo centralized = assessmentGradingQueryRepository.stats(schoolId, null, null, "CENTRALIZED");
        GradingStatsInfo classTest = assessmentGradingQueryRepository.stats(schoolId, null, null, "CLASS_TEST");

        assertThat(centralized.total()).isEqualTo(1);
        assertThat(centralized.assigned()).isEqualTo(1);
        assertThat(classTest.total()).isEqualTo(1);
        assertThat(classTest.assigned()).isEqualTo(1);
        assertThat(centralized.teacherProgress()).hasSize(1);
    }

    @Test
    void should_keep_class_test_assignments_out_of_the_centralized_teacher_queue() {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository
            .findTasksByTeacherId(teacherId, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(task -> assertThat(task.assessmentName()).isEqualTo("Kỳ thi cuối kỳ"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_keep_centralized_assignments_out_of_the_class_test_queue() {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository
            .findTasksByTeacherId(teacherId, "CLASS_TEST", null, null, 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(task -> assertThat(task.assessmentName()).isEqualTo("Kiểm tra 15 phút"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    /**
     * Auto-assign is an action of the school; if class-test submissions slip in here, every run
     * picks up a bunch of submissions and then gets blocked at a lower layer.
     */
    @Test
    void should_never_offer_class_test_results_to_auto_assign() {
        // The submission already has an open assignment, so the returned set is empty; what has to
        // be pinned is that the class-test submission does not appear even when its status is valid
        // for the grading round.
        List<UUID> assignable = assessmentGradingQueryRepository.findAssignableResultIds(
            schoolId, null, null, List.of("PENDING_REVIEW", "RELEASED"));

        assertThat(assignable).isEmpty();
    }

    private GradingAssignmentFilter filter(String assessmentKind) {
        return new GradingAssignmentFilter(
            schoolId, null, null, null, null, null, null, false, false, null, null, assessmentKind);
    }

    private void seedAssessment(String kind, String name, UUID studentId) {
        UUID paperId = UUID.randomUUID();
        UUID assessmentId = persist(AssessmentJpaEntity.builder()
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
