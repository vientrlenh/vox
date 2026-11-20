package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * An assessment seen from the PROCTORING screen -- deliberately much smaller than {@code AssessmentDto}.
 *
 * <p>Separated from the assessment management DTO because the two screens have two different sets of viewers. A proctor assigned to
 * a session needs exactly this much to recognize their room; they do not need (and must not) see the
 * blueprint, the grading policy, the AI threshold or the stream permission that {@code AssessmentDto} carries. Sharing
 * one DTO would mean every field later added for the management screen automatically flows to proctors.
 *
 * @param windowStart the EARLIEST start time among the sessions the viewer can proctor, not the
 *                    {@code openAt} of the assessment -- an assessment may be open all day while this person's session
 *                    only runs for one hour.
 * @param liveScheduleCount number of sessions running RIGHT NOW. 0 means the assessment made it into the list because
 *                    it is about to start, and the screen must say so instead of inviting a click to watch live.
 */
public record MonitoredAssessmentSummary(
    UUID assessmentId,
    String code,
    String name,
    String kind,
    String status,
    Instant windowStart,
    Instant windowEnd,
    long liveScheduleCount
) {
}
