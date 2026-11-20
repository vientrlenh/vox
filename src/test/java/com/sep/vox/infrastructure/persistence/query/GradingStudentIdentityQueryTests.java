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
import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;
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
        UUID studentId = persist(new UserJpaEntity(null, "hs-" + UUID.randomUUID() + "@vox.test", "hash",
            null, "Trần Quang Thiên", null, LocalDate.of(2008, 1, 1), null, null, "ACTIVE",
            now, now, null, null)).getId();
        UUID schoolClassId = persist(new SchoolClassJpaEntity(schoolId, UUID.randomUUID(), UUID.randomUUID(),
            "10A1-" + UUID.randomUUID().toString().substring(0, 8), "10A1", null, "ACTIVE",
            now, now, teacherId, teacherId)).getId();
        persist(new SchoolClassUserJpaEntity(studentId, schoolClassId, true, now, null, teacherId));

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
        UUID assessmentId = persist(new AssessmentJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), kind, "STUDENT_DEVICE", "CLOSED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
        UUID candidateId = persist(new AssessmentCandidateJpaEntity(null, assessmentId, studentId, paperId, null,
            "COMPLETED", now, now, null, null, null)).getId();
        UUID sessionId = persist(new AssessmentSessionJpaEntity(null, assessmentId, candidateId, paperId, now, now,
            "GRADED", false, null)).getId();
        UUID candidateResultId = persist(new AssessmentCandidateResultJpaEntity(null, assessmentId, candidateId, sessionId,
            UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
            new BigDecimal("6.50"), "PENDING_REVIEW", null, null, now, now, null, null)).getId();
        persist(new AssessmentGradingAssignmentJpaEntity(null, candidateResultId, teacherId,
            "INITIAL", null, "ASSIGNED", null, new BigDecimal("6.50"), now, teacherId, null,
            null, null, null, candidateResultId));
        return assessmentId;
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
