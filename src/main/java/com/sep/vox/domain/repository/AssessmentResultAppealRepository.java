package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;

public interface AssessmentResultAppealRepository {
    Optional<AssessmentResultAppeal> findById(UUID id);
    AssessmentResultAppeal save(AssessmentResultAppeal appeal);
    boolean existsOpenByCandidateResultId(UUID candidateResultId);
    /** Number of appeal rounds actually re-graded and published — rejected appeals do not count. */
    long countPublishedByCandidateResultId(UUID candidateResultId);
    List<AssessmentResultAppeal> findByCandidateResultId(UUID candidateResultId);
    void deleteByIdIn(Collection<UUID> ids);
    long countBySchoolIdAndStatusIn(UUID schoolId, Collection<AssessmentAppealStatus> statuses);

    /** Submission time of the longest-waiting pending appeal of the school; null when the queue is clean — null is NOT 0 days. */
    Instant findOldestPendingRequestedAt(UUID schoolId);
}
