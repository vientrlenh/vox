package com.sep.vox.domain.model.assessment;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AssessmentSessionStatus {
    IN_PROGRESS,
    SUBMITTED,
    INTERRUPTED,
    GRADING,
    GRADED,
    EXPIRED, // submission time ran out
    GRADING_FAILED, // AI grading failed
    /**
     * Soft-deleted (see {@code DeleteAssessmentSessionUseCase}). Not a step in the lifecycle of an assessment attempt
     * but a stopping point.
     *
     * <p>NOT filtered automatically by Hibernate -- {@code AssessmentSessionJpaEntity} deliberately does not use
     * {@code @SQLRestriction} (see the note there), because the school administrator/council chair must still
     * read a deleted session to account for it. That means {@code findByCandidateId} and every other query
     * on this table STILL RETURN DELETED rows -- whoever writes a new query/business flow has to exclude
     * it themself when needed, and there is no safety net at the ORM layer to do it for them.
     *
     * <p>It was once wrongly believed to have {@code @SQLRestriction} (the old note here said so, which was wrong in
     * reality) -- {@code UpdateAssessmentStatusUseCase.requirePublishReadiness} was the real victim of that
     * misunderstanding: without a DELETED filter, a student who failed the attempt, had the session deleted and retook it would block the publishing of
     * the results of the WHOLE ASSESSMENT permanently.
     */
    DELETED;

    /**
     * The statuses in which an assessment session is still considered usable: the student can re-enter,
     * and the system must still accept the monitoring data of that session.
     *
     * <p>Defined here instead of writing {@code EnumSet.of(...)} at each place of use, because previously the two
     * places drifted apart: the enter-exam flow treated INTERRUPTED as re-enterable, while the stream token
     * issuing flow only accepted IN_PROGRESS - so precisely the situation that needs it most (the student's machine
     * loses connection and comes back with recording chunks sitting in the buffer) was the one where no token could
     * be obtained to push the data up.
     */
    public static final Set<AssessmentSessionStatus> RESUMABLE =
        Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS, INTERRUPTED));
}
