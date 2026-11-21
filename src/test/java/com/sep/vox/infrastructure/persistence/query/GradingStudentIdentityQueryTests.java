package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.user.UserJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Student identity on the grading screen: <strong>shown for a class test, hidden for a
 * CENTRALIZED assessment.</strong>
 *
 * <p>The control case {@code should_keep_student_anonymous_for_centralized_assessment} is the
 * most important part of this class. Without it, a later edit to the query could leak the identity
 * of a centralized assessment -- i.e. break blind grading, the thing that guarantees fairness -- without any
 * test going red.
 *
 * <p>It must run on real Postgres because this is JPQL built with {@code em.createQuery}: a wrong
 * field name only blows up at CALL time and not at compile time.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class GradingStudentIdentityQueryTests {

    @Autowired
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private UUID classTestAssessmentId;
    private UUID centralizedAssessmentId;

    @BeforeEach
    void seed() {
        UUID schoolId = UUID.randomUUID();
        UUID studentId = persist(UserJpaEntity.builder()
            .email("hs-" + UUID.randomUUID() + "@vox.test")
            .passwordHash("hash")
            .fullName("Trần Quang Thiên")
            .dateOfBirth(LocalDate.of(2008, 1, 1))
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build()).getId();
        UUID schoolClassId = persist(SchoolClassJpaEntity.builder()
            .schoolId(schoolId)
            .languageId(UUID.randomUUID())
            .schoolGradeId(UUID.randomUUID())
            .code("10A1-" + UUID.randomUUID().toString().substring(0, 8))
            .name("10A1")
            .description(null)
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .createdBy(teacherId.toString())
            .updatedBy(teacherId.toString())
            .build()).getId();
        persist(SchoolClassUserJpaEntity.builder()
            .userId(studentId)
            .schoolClassId(schoolClassId)
            .isActive(true)
            .joinedAt(now)
            .leftAt(null)
            .assignedBy(teacherId)
            .build());

        classTestAssessmentId = seedAssessment(schoolId, "CLASS_TEST", "Kiểm tra 15 phút", studentId);
        centralizedAssessmentId = seedAssessment(schoolId, "CENTRALIZED", "Kỳ thi cuối kỳ", studentId);

        em.flush();
        em.clear();
    }

    @Test
    void should_expose_student_and_class_for_class_test_task() {
        GradingTaskInfo task = onlyTaskOf(classTestAssessmentId);

        assertThat(task.studentName()).isEqualTo("Trần Quang Thiên");
        assertThat(task.className()).isEqualTo("10A1");
    }

    /** Control case: blind grading of a centralized assessment must not leak. */
    @Test
    void should_keep_student_anonymous_for_centralized_assessment() {
        GradingTaskInfo task = onlyTaskOf(centralizedAssessmentId);

        assertThat(task.studentName()).isNull();
        assertThat(task.className()).isNull();
        // The submission code is still the only thing that identifies a submission on the anonymous
        // screen.
        assertThat(task.resultCode()).isNotBlank();
    }

    /**
     * A queue that does NOT filter by assessment kind mixes both -- each row must follow the rules of its
     * own assessment, not the rules of the first row on the page. Every use case passes the kind, so this
     * mixing only remains at the repository layer, but the per-row rule must still be correct.
     */
    @Test
    void should_decide_per_row_when_the_queue_mixes_both_kinds() {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository.findTasksByTeacherId(teacherId, null, null, null, 1, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content())
            .filteredOn(task -> "Kiểm tra 15 phút".equals(task.assessmentName()))
            .singleElement()
            .satisfies(task -> assertThat(task.studentName()).isEqualTo("Trần Quang Thiên"));
        assertThat(page.content())
            .filteredOn(task -> "Kỳ thi cuối kỳ".equals(task.assessmentName()))
            .singleElement()
            .satisfies(task -> assertThat(task.studentName()).isNull());
    }

    @Test
    void should_expose_student_and_class_on_the_grading_detail_of_a_class_test() {
        GradingTaskDetailInfo detail = assessmentGradingQueryRepository
            .findTaskDetail(onlyTaskOf(classTestAssessmentId).assignmentId(), teacherId).orElseThrow();

        assertThat(detail.studentName()).isEqualTo("Trần Quang Thiên");
        assertThat(detail.className()).isEqualTo("10A1");
    }

    @Test
    void should_keep_the_grading_detail_anonymous_for_a_centralized_assessment() {
        GradingTaskDetailInfo detail = assessmentGradingQueryRepository
            .findTaskDetail(onlyTaskOf(centralizedAssessmentId).assignmentId(), teacherId).orElseThrow();

        assertThat(detail.studentName()).isNull();
        assertThat(detail.className()).isNull();
    }

    private GradingTaskInfo onlyTaskOf(UUID assessmentId) {
        PageResult<GradingTaskInfo> page = assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            teacherId, assessmentId, null, null, null, 1, 20);
        assertThat(page.content()).hasSize(1);
        return page.content().get(0);
    }

    private UUID seedAssessment(UUID schoolId, String kind, String name, UUID studentId) {
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
        return assessmentId;
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
