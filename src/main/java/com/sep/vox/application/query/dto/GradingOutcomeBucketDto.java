package com.sep.vox.application.query.dto;

import java.time.LocalDate;

/**
 * The AI grading outcome of ONE calendar day, with the success / failure dimensions already combined.
 *
 * <p>The day here is the calendar day in Vietnam time ({@code ZoneConstant.BUSINESS_ZONE}), not the UTC
 * day: cutting by UTC pushes the first 7 hours of each day into the previous day, so early-morning
 * assessment sessions land in the wrong column.
 */
public record GradingOutcomeBucketDto(
    LocalDate day,
    long graded,
    long failed
) {
}
