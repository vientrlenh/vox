package com.sep.vox.application.response.input.assessmentgrading;

import java.util.List;
import java.util.UUID;

/**
 * The result of one pass of reclaiming overdue assignments.
 *
 * <p>Two separate lists instead of one {@code List<UUID>} carrying two meanings depending on the branch:
 * before, the same endpoint returned the ids of assignments that were <em>closed</em> when only reclaiming, and
 * the ids of <em>newly created</em> assignments when reassigning -- the client had no way to tell them apart.
 *
 * @param reclaimedAssignmentIds old assignments just closed (always present when anything was reclaimed)
 * @param reassignedAssignmentIds new assignments just opened for the replacement group; empty when the admin
 *        only reclaimed without choosing anyone
 * @param hasMore overdue assignments remain unprocessed because the cap of one pass was reached -- the admin
 *        clicks again to run the next pass. Without this flag a truncated "reclaim everything" pass
 *        looks exactly like one that finished.
 */
public record ReclaimOverdueResponse(
    List<UUID> reclaimedAssignmentIds,
    List<UUID> reassignedAssignmentIds,
    boolean hasMore
) {
    public static ReclaimOverdueResponse empty() {
        return new ReclaimOverdueResponse(List.of(), List.of(), false);
    }
}
