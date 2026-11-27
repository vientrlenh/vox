package com.sep.vox.application.port.input.query.question;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

/**
 * Like {@link ViewQuestionsQuery} but without {@code status} (the dimension being grouped) and
 * {@code page}/{@code size} (the result is one summary row).
 *
 * <p>The remaining parameters must keep the SAME list as ViewQuestionsQuery: the screen uses
 * one shared filter for both the count chart and the list, and missing one parameter here means the two sides
 * produce two different numbers.
 */
public record ViewQuestionStatusCountsQuery(
    UUID questionBankId,
    UUID questionTopicId,
    String topicName,
    QuestionType type,
    QuestionSharing sharing,
    String scope,
    String keyword
) {
}
