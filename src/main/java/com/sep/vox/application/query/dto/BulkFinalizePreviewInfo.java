package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.UUID;

/**
 * A snapshot before the admin closes out a whole assessment.
 *
 * <p>It exists because {@code finalizeForPublish} skips {@code PENDING_REVIEW} submissions: a single
 * submission that nobody has graded is enough to stop the whole assessment from being published, and
 * before this the admin had no way to see which submission it was (review BE-5). The preview returns
 * exactly the numbers and the list so they can decide: finish grading, or accept publishing with the
 * AI scores currently held.
 *
 * @param pendingUnassigned  submissions waiting for grading with NO one having taken them
 * @param pendingAssigned    submissions waiting for grading that someone is holding
 * @param openAppeals        appeals that are not yet finished
 * @param invalid            submissions already invalidated (the close-out does not touch them; they become FAILED
 *                           with score 0 when the assessment publishes its results)
 * @param blockingResultIds  the list of blocking submissions, truncated for the UI
 */
public record BulkFinalizePreviewInfo(
    int total,
    int readyToFinalize,
    int pendingUnassigned,
    int pendingAssigned,
    int openAppeals,
    int invalid,
    List<UUID> blockingResultIds
) {
    /** Can be finalized directly without the admin choosing how to handle unfinished submissions. */
    public boolean isClean() {
        return pendingUnassigned == 0 && pendingAssigned == 0 && openAppeals == 0;
    }
}
