package com.sep.vox.application.response.input.assessmentgrading;

import java.util.List;
import java.util.UUID;


public record ReclaimOverdueResponse(
    List<UUID> reclaimedAssignmentIds,
    List<UUID> reassignedAssignmentIds,
    boolean hasMore
) {
    public static ReclaimOverdueResponse empty() {
        return new ReclaimOverdueResponse(List.of(), List.of(), false);
    }
}
