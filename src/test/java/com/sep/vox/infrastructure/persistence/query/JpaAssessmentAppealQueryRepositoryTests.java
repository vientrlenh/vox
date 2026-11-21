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
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.user.UserJpaEntity;
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
        studentId = persist(UserJpaEntity.builder()
            .email("hs-" + UUID.randomUUID() + "@vox.test")
            .passwordHash("hash")
            .fullName("Tran Quang Thien")
            .dateOfBirth(LocalDate.of(2008, 1, 1))
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build()).getId();
        seedAppeal("PENDING", null, null, null);
        seedAppeal("GRADING", "ASSIGNED", null, now.minusSeconds(3_600));
        em.flush();
        em.clear();
    }

    @Test
    void should_return_the_enum_status_and_the_reviewer_enum_status() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, null, null, 1, 20);

        assertThat(page.content()).extracting(info -> info.status())
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

        assertThat(page.content()).extracting(info -> info.status()).containsExactly(AssessmentAppealStatus.GRADING);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_flag_only_an_in_progress_appeal_past_its_deadline_as_overdue() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, null, null, 1, 20);

        assertThat(page.content()).filteredOn(info -> info.overdue())
            .extracting(info -> info.status()).containsExactly(AssessmentAppealStatus.GRADING);
    }

    @Test
    void should_list_and_filter_the_appeals_of_a_student_by_the_enum_status() {
        PageResult<AppealSummaryInfo> page = assessmentAppealQueryRepository.searchAppealsByStudentId(
            studentId, AssessmentAppealStatus.PENDING, 1, 20);

        assertThat(page.content()).extracting(info -> info.status()).containsExactly(AssessmentAppealStatus.PENDING);
    }

    private void seedAppeal(String appealStatus, String reviewerStatus, String outcome, Instant deadline) {
        UUID paperId = UUID.randomUUID();
        UUID assessmentId = persist(AssessmentJpaEntity.builder()
                .blueprintId(null)
                .blueprintVersionId(null)
                .code("EX-" + UUID.randomUUID())
                .name("Midterm")
                .description(null)
                .schoolId(schoolId)
                .languageId(UUID.randomUUID())
                .kind("CENTRALIZED")
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
                .status("APPEALED")
                .releasedAt(null)
                .finalizedAt(null)
                .build()).getId();
        UUID appealId = persist(AssessmentResultAppealJpaEntity.builder()
                .id(null)
                .candidateResultId(candidateResultId)
                .requestedBy(studentId)
                .reason("reason")
                .requestedAt(now)
                .status(appealStatus)
                .scoreBefore(new BigDecimal("6.50"))
                .scoreAfter(null)
                .resolvedBy(null)
                .resolvedAt(null)
                .notes(null)
                .deadline(deadline)
                .approvedAt(null)
                .decisionNote(null)
                .withdrawnAt(null)
                .reviewerOverrideReason(null)
                .build()).getId();
        if (reviewerStatus != null) {
            persist(AssessmentGradingAssignmentJpaEntity.builder()
                    .id(null)
                    .candidateResultId(candidateResultId)
                    .teacherId(teacherId)
                    .roundType("APPEAL")
                    .appealId(appealId)
                    .status(reviewerStatus)
                    .outcome(outcome)
                    .scoreBefore(new BigDecimal("6.50"))
                    .assignedAt(now)
                    .assignedBy(teacherId)
                    .completedAt(null)
                    .deadlineAt(deadline)
                    .remindedAt(null)
                    .reason(null)
                    .activeResultId(candidateResultId)
                    .build());
        }
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
