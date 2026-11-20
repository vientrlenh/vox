package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * The teacher's queue -- ONE list for all four rounds, which can be filtered further by
 * {@code roundType} if they want to look at one round separately. 0-based paging.
 *
 * <p>{@code assessmentId} only NARROWS, it is not a permission gate: the scope is still the caller's own
 * assignments, so passing the id of an assessment in which they have no attempt gives an empty
 * list and reveals nothing.
 */
public record ViewMyGradingTasksQuery(
    UUID assessmentId,
    String status,
    String roundType,
    int page,
    int size
) {
}
