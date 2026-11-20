package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * 0-based paging, in line with the other queries of the same assessment-appeal domain.
 *
 * <p>After the rework this table is no longer hard-locked to PENDING_REVIEW, so it gained the coordination
 * filter dimensions: attempt status, grading round, unassigned, overdue, has an appeal.
 *
 * @param kind kind of assessment; {@code null} is interpreted by the use case as {@code CENTRALIZED}. The default
 *             leans towards the centralized assessment because this is the school's coordination table -- class
 *             tests only come here when the client asks for them explicitly (the monitoring screen).
 */
public record SearchGradingAssignmentsQuery(
    UUID assessmentId,
    UUID scheduleId,
    UUID teacherId,
    String resultStatus,
    String roundType,
    String status,
    boolean unassignedOnly,
    boolean overdueOnly,
    Boolean hasOpenAppeal,
    String search,
    String kind,
    int page,
    int size
) {
}
