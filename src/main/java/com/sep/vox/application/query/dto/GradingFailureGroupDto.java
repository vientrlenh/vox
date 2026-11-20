package com.sep.vox.application.query.dto;

import java.time.Instant;

/**
 * A GROUP of failed grading sessions with the same cause.
 *
 * @param signature       the normalized signature of the error message; {@code null} is the "unknown
 *                        cause" group -- a real group, not missing data (see V6)
 * @param sampleError     one representative raw message of the group, to show verbatim to the reader;
 *                        {@code null} for the unknown-cause group
 * @param sessionCount    number of sessions in the group
 * @param schoolCount     number of distinct schools; a system-level assessment belongs to no school so it is
 *                        not counted
 * @param assessmentCount number of distinct assessments
 * @param firstFailedAt   the earliest submission time in the group -- together with the latest, this is what shows
 *                        that an incident contained in two hours differs from a slow-burning error lasting two weeks
 * @param lastFailedAt    the latest submission time in the group
 * @param retryableCount  number of sessions that can really be regraded -- see {@link GradingFailureTotalsDto}
 */
public record GradingFailureGroupDto(
    String signature,
    String sampleError,
    long sessionCount,
    long schoolCount,
    long assessmentCount,
    Instant firstFailedAt,
    Instant lastFailedAt,
    long retryableCount
) {
}
