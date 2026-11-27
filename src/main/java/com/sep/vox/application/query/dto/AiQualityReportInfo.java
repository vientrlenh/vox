package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * "How far off is the AI grading", measured with the post-review data that already exists -- nothing
 * extra is collected.
 *
 * <p>The denominator is the completed {@code SPOT_CHECK} rounds: a teacher actually re-read the
 * submission and reached a conclusion. {@code INITIAL} rounds do not count because the AI is not
 * considered final there.
 *
 * @param upheld      number of submissions where the teacher confirmed the AI score was right
 * @param regraded    number of submissions where the teacher had to change the score
 * @param invalidated number of submissions found to be violations
 * @param averageDelta average absolute difference between the score at assignment time and the score
 *                     after re-grading; counted only over {@code REGRADED} submissions
 */
public record AiQualityReportInfo(
    int reviewed,
    int upheld,
    int regraded,
    int invalidated,
    BigDecimal regradeRate,
    BigDecimal averageDelta,
    BigDecimal maxDelta,
    List<ByTeacher> byTeacher
) {
    public record ByTeacher(
        UUID teacherId,
        String teacherName,
        int reviewed,
        int regraded,
        BigDecimal averageDelta
    ) {
    }
}
