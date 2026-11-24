package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.infrastructure.persistence.adapter.AssessmentCandidateRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RepositoryTest
@Import(AssessmentCandidateRepositoryImpl.class)
class AssessmentCandidateFirstPerAssessmentTests {

    private final Instant baseTime = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private AssessmentCandidateRepository assessmentCandidateRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void should_return_the_earliest_assigned_candidate_of_each_assessment_and_skip_assessments_without_candidates() {
        UUID assessmentWithFive = seedAssessment();
        UUID assessmentWithOne = seedAssessment();
        UUID assessmentWithNone = seedAssessment();
        UUID earliestOfFive = seedCandidates(assessmentWithFive, 5);
        UUID onlyOfOne = seedCandidates(assessmentWithOne, 1);
        entityManager.flush();
        entityManager.clear();

        List<AssessmentCandidate> result = assessmentCandidateRepository.findFirstCandidateByAssessmentIdIn(
            List.of(assessmentWithFive, assessmentWithOne, assessmentWithNone));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(c -> c.getAssessmentId())
            .containsExactlyInAnyOrder(assessmentWithFive, assessmentWithOne);
        assertThat(result).extracting(c -> c.getStudentId())
            .containsExactlyInAnyOrder(earliestOfFive, onlyOfOne);
    }

    @Test
    void should_return_nothing_for_an_empty_assessment_id_collection() {
        assertThat(assessmentCandidateRepository.findFirstCandidateByAssessmentIdIn(List.of())).isEmpty();
    }

    /** Seeds {@code count} candidates assigned one minute apart, in reverse order, and returns the student id of the earliest. */
    private UUID seedCandidates(UUID assessmentId, int count) {
        UUID earliestStudentId = null;
        for (int index = count - 1; index >= 0; index--) {
            UUID studentId = UUID.randomUUID();
            entityManager.persist(AssessmentCandidateJpaEntity.builder()
                .id(null)
                .assessmentId(assessmentId)
                .studentId(studentId)
                .assignedPaperId(null)
                .scheduleId(null)
                .status("ASSIGNED")
                .assignedAt(baseTime.plus(index, ChronoUnit.MINUTES))
                .updatedAt(baseTime)
                .blockedAt(null)
                .assignedBy(null)
                .updatedBy(null)
                .build());
            earliestStudentId = index == 0 ? studentId : earliestStudentId;
        }
        return earliestStudentId;
    }

    private UUID seedAssessment() {
        AssessmentJpaEntity assessment = AssessmentJpaEntity.builder()
            .blueprintId(null)
            .blueprintVersionId(null)
            .code("EX-" + UUID.randomUUID())
            .name("Assessment")
            .description(null)
            .schoolId(UUID.randomUUID())
            .languageId(UUID.randomUUID())
            .kind("CLASS_TEST")
            .deliveryMode("STUDENT_DEVICE")
            .status("SCHEDULED")
            .maxAttempt(1)
            .assessmentTimeDurationSecond(900)
            .resultDecisionMethod("HIGHEST")
            .requiredStreamType(null)
            .streamTypePermission(null)
            .openAt(null)
            .closeAt(null)
            .assessmentPolicyId(UUID.randomUUID())
            .requiresOtp(false)
            .build();
        entityManager.persist(assessment);
        return assessment.getId();
    }
}
