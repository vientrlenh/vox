package com.sep.vox.application.query.dto;

import com.sep.vox.domain.model.question.QuestionStatus;

/**
 * The number of questions of ONE status, within the scope the caller may see.
 *
 * <p>Returns a list of rows like this instead of a record with one field per status: adding a new
 * constant to {@link QuestionStatus} then does not require changing the DTO, the GraphQL schema, or
 * any query.
 *
 * <p>In exchange, {@code GROUP BY} produces no row for a status that has no questions. Filling in all
 * seven statuses (including count = 0) is the responsibility of the use case layer -- the client needs
 * a stable axis to draw a chart, not a list that stretches with the data.
 */
public record QuestionStatusCountInfo(
    QuestionStatus status,
    long count
) {
}
