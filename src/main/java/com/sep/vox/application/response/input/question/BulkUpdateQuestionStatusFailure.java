package com.sep.vox.application.response.input.question;

import java.util.UUID;

/**
 * One question skipped in a bulk update.
 *
 * <p>Besides {@code reason} (human-readable text shown directly to the user) there is {@code reasonCode} so the
 * client can group questions with the same reason, and {@code questionCode}/{@code currentStatus} so the client can show
 * the list of skipped questions without looking back into the list being viewed -- before, it had to
 * join by id itself, so a question not on the current page would vanish from the message.
 *
 * @param questionCode {@code null} when the question is not found
 * @param currentStatus the {@code QuestionStatus} enum name, {@code null} when the question is not found
 * @param reasonCode the {@code QuestionStatusTransition.RejectionCode} enum name
 */
public record BulkUpdateQuestionStatusFailure(
    UUID questionId,
    String questionCode,
    String currentStatus,
    String reasonCode,
    String reason
) {
}
