package com.sep.vox.application.response.input.dashboard;

import java.time.Instant;

import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.domain.common.BusinessDays;

/**
 * Submissions that are finished but the student has no score yet, split by what is blocking them.
 *
 * <p>The five buckets are mutually exclusive and add up to exactly {@code total} -- see
 * {@link SchoolUnscoredWorkloadDto} for the definition of each bucket.
 *
 * <p>{@code aiFailed} is the sum of the two AI buckets, returned ready-made because the UI merges them into ONE row while the two
 * sub-numbers are only secondary labels on that row. If the client adds them itself, the number on the card and the number after clicking
 * through can disagree if one side forgets a bucket.
 */
public record UnscoredWorkloadResponse(
    int total,
    int aiFailed,
    int aiFailedRetryLeft,
    int aiFailedNoRetryLeft,
    int awaitingAssignment,
    int assignedOverdue,
    int assignedInProgress,
    /** How many calendar days the longest-waiting submission has waited; null when no submission is left. */
    Integer oldestWaitingDays,
    int assessmentCount
) {

    public static UnscoredWorkloadResponse of(SchoolUnscoredWorkloadDto dto, Instant now) {
        return new UnscoredWorkloadResponse(
            dto.total(),
            dto.aiFailed(),
            dto.aiFailedRetryLeft(),
            dto.aiFailedNoRetryLeft(),
            dto.awaitingAssignment(),
            dto.assignedOverdue(),
            dto.assignedInProgress(),
            BusinessDays.waitedDaysSince(dto.oldestSubmittedAt(), now),
            dto.assessmentCount()
        );
    }
}
