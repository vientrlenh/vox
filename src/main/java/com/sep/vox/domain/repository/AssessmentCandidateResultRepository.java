package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;

public interface AssessmentCandidateResultRepository {
    Optional<AssessmentCandidateResult> findById(UUID id);
    List<AssessmentCandidateResult> findByIdIn(Collection<UUID> ids);
    Optional<AssessmentCandidateResult> findBySessionId(UUID sessionId);
    List<AssessmentCandidateResult> findBySessionIdIn(Collection<UUID> sessionIds);
    List<AssessmentCandidateResult> findByAssessmentId(UUID assessmentId);
    AssessmentCandidateResult save(AssessmentCandidateResult result);
    void deleteBySessionId(UUID sessionId);

    /** Soft-deletes the results of an assessment session, together with the session itself. */
    int softDeleteBySessionId(UUID sessionId, Instant deletedAt, String reason);
}
