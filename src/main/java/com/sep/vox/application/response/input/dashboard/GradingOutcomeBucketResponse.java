package com.sep.vox.application.response.input.dashboard;

import java.time.LocalDate;

/**
 * One column of the AI grading health chart.
 *
 * <p>{@code day} is a calendar day in Vietnam time -- deliberately NOT an instant: the client only uses this value
 * as an axis label and a grouping key, and with an instant the client would have to convert it back to a day, and
 * would do so in the browser's time zone instead of the business time zone.
 */
public record GradingOutcomeBucketResponse(
    LocalDate day,
    long graded,
    long failed
) {
}
