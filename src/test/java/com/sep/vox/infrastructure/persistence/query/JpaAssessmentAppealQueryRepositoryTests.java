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

import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentResultAppealJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The appeal list is built with {@code em.createQuery} and turns the stored status strings into Java enums
 * ({@code AssessmentAppealStatus}, {@code GradingAssignmentStatus}). Only real rows on Postgres prove that every stored
 * value converts, that the enum filter is bound as its name, and that the overdue rule still reads the enum.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class JpaAssessmentAppealQueryRepositoryTests {

    @Autowired
    private AssessmentAppealQueryRepository assessmentAppealQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private UUID schoolId;
    private UUID studentId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();
        studentId = persist(new UserJpaEntity(null, "hs-" + UUID.randomUUID() + "@vox.test", "hash",
            null, "Tran Quang Thien", null, LocalDate.of(2008, 1, 1), null, null, "ACTIVE",
            now, now, null, null)).getId();
        seedAppeal("PENDING", null, null, null);
        seedAppeal("GRADING", "ASSIGNED", null, now.minusSeconds(3_600));
        em.flush();
        em.clear();
    }

    @Test
    void should_return_the_enum_status_and_the_reviewer_enum_status() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, null, null, 1, 20);

        assertThat(page.content()).extracting(AppealSummaryInfo::status)
            .containsExactlyInAnyOrder(AssessmentAppealStatus.PENDING, AssessmentAppealStatus.GRADING);
        assertThat(page.content()).filteredOn(appeal -> appeal.status() == AssessmentAppealStatus.GRADING)
            .singleElement()
            .satisfies(appeal -> assertThat(appeal.reviewerStatus()).isEqualTo(GradingAssignmentStatus.ASSIGNED));
        assertThat(page.content()).filteredOn(appeal -> appeal.status() == AssessmentAppealStatus.PENDING)
            .singleElement()
            .satisfies(appeal -> assertThat(appeal.reviewerStatus()).isNull());
    }

    @Test
    void should_filter_by_the_enum_status() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, AssessmentAppealStatus.GRADING, null, 1, 20);

        assertThat(page.content()).extracting(AppealSummaryInfo::status).containsExactly(AssessmentAppealStatus.GRADING);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_flag_only_an_in_progress_appeal_past_its_deadline_as_overdue() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, null, null, 1, 20);

        assertThat(page.content()).filteredOn(AppealSummaryInfo::overdue)
            .extracting(AppealSummaryInfo::status).containsExactly(AssessmentAppealStatus.GRADING);
    }

    @Test
    void should_list_and_filter_the_appeals_of_a_student_by_the_enum_status() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppealsByStudentId(
            studentId, AssessmentAppealStatus.PENDING, 1, 20);

        assertThat(page.content()).extracting(AppealSummaryInfo::status).containsExactly(AssessmentAppealStatus.PENDING);
    }

    private void seedAppeal(String appealStatus, String reviewerStatus, String outcome, Instant deadline) {
        UUID paperId = UUID.randomUUID();
        UUID assessmentId = persist(new AssessmentJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), "Midterm",
            null, schoolId, UUID.randomUUID(), "CENTRALIZED", "STUDENT_DEVICE", "CLOSED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
        UUID candidateId = persist(new AssessmentCandidateJpaEntity(null, assessmentId, studentId, paperId, null,
            "COMPLETED", now, now, null, null, null)).getId();
        UUID sessionId = persist(new AssessmentSessionJpaEntity(null, assessmentId, candidateId, paperId, now, now,
            "GRADED", false, null)).getId();
        UUID candidateResultId = persist(new AssessmentCandidateResultJpaEntity(null, assessmentId, candidateId,
            sessionId, UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
            new BigDecimal("6.50"), "APPEALED", null, null, now, now, null, null)).getId();
        UUID appealId = persist(new AssessmentResultAppealJpaEntity(null, candidateResultId, studentId, "reason", now,
            appealStatus, new BigDecimal("6.50"), null, null, null, null, deadline, null, null, null, null)).getId();
        if (reviewerStatus != null) {
            persist(new AssessmentGradingAssignmentJpaEntity(null, candidateResultId, teacherId, "APPEAL", appealId,
                reviewerStatus, outcome, new BigDecimal("6.50"), now, teacherId, null, deadline, null, null,
                candidateResultId));
        }
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
