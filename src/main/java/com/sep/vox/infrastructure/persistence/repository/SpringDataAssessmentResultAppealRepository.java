package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealJpaEntity;

public interface SpringDataAssessmentResultAppealRepository extends JpaRepository<AssessmentResultAppealJpaEntity, UUID> {

    /**
     * Appeals that still occupy the slot. Lists the open statuses EXPLICITLY instead of
     * {@code NOT IN} the closed statuses: every time a new terminal status is added
     * (the latest was {@code WITHDRAWN}) and {@code NOT IN} is forgotten, the student is locked out of
     * filing a new appeal and nobody notices.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM AssessmentResultAppealJpaEntity a
        WHERE a.candidateResultId = :candidateResultId
        AND a.status IN ('PENDING', 'APPROVED', 'GRADING')
    """)
    boolean existsOpenByCandidateResultId(@Param("candidateResultId") UUID candidateResultId);

    /** The appeal quota counts published rounds; a rejected appeal does not use up an attempt. */
    @Query("""
        SELECT COUNT(a) FROM AssessmentResultAppealJpaEntity a
        WHERE a.candidateResultId = :candidateResultId
        AND a.status = 'PUBLISHED'
    """)
    long countPublishedByCandidateResultId(@Param("candidateResultId") UUID candidateResultId);

    List<AssessmentResultAppealJpaEntity> findByCandidateResultId(UUID candidateResultId);

    /** candidateResultId/assessmentId are bare FK columns with no JPA relationship -- an explicit join across 3 tables. */
    @Query("""
        SELECT COUNT(a) FROM AssessmentResultAppealJpaEntity a, AssessmentCandidateResultJpaEntity r, AssessmentJpaEntity e
        WHERE a.candidateResultId = r.id
        AND r.assessmentId = e.id
        AND e.schoolId = :schoolId
        AND a.status IN :statuses
    """)
    long countBySchoolIdAndStatusIn(@Param("schoolId") UUID schoolId, @Param("statuses") Collection<String> statuses);

    /**
     * Submission time of the longest-waiting PENDING appeal; null when no appeal is waiting.
     *
     * <p>Only {@code PENDING}: appeals that are {@code APPROVED}/{@code GRADING} are being graded, so their clock
     * measures something else -- grading time, not the time the school neglected the appeal.
     *
     * <p>null is NOT 0 and that difference must reach the UI: 0 days means an appeal was just submitted this
     * morning, while null means the queue is clean. Merging the two would draw an empty queue that looks exactly like
     * a queue that just received an appeal.
     */
    @Query("""
        SELECT MIN(a.requestedAt) FROM AssessmentResultAppealJpaEntity a, AssessmentCandidateResultJpaEntity r, AssessmentJpaEntity e
        WHERE a.candidateResultId = r.id
        AND r.assessmentId = e.id
        AND e.schoolId = :schoolId
        AND a.status = 'PENDING'
    """)
    Instant findOldestPendingRequestedAtBySchoolId(@Param("schoolId") UUID schoolId);
}
