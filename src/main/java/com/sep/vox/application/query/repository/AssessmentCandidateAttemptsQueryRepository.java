package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssessmentAttemptSummary;

/**
 * The attempts of a candidate.
 *
 * <p>The two methods are split by WHO READS, not by data: a soft-deleted session is something the school
 * admin and the council chair must be able to see (to answer "where did my score go"), but the student must not.
 * Split into two clearly named methods instead of a boolean flag so that every call site has to say whom it serves.
 */
public interface AssessmentCandidateAttemptsQueryRepository {

    /** The safe default: EXCLUDES soft-deleted attempts. Used for every path a student can see. */
    List<AssessmentAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds);

    /**
     * Includes soft-deleted attempts too (with {@code status = DELETED} and {@code deletedReason}).
     *
     * <p>ONLY for read paths already restricted to the school administrator / council chair -- currently
     * {@code ResolveAssessmentCandidateAttemptsUseCase}, serving the assessment results screen.
     */
    List<AssessmentAttemptSummary> findByCandidateIdsIncludingDeleted(Collection<UUID> candidateIds);
}
