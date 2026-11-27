package com.sep.vox.application.response.input.question;

import java.util.UUID;

/**
 * One item skipped in a bulk status change.
 *
 * <p>Comes with {@code code} and {@code currentStatus} so the client can show "which item, where it is, why"
 * without looking back into the list being viewed -- an item that is not on the current page still shows
 * full information. {@code reasonCode} is for grouping by reason.
 *
 * @param code the code of the bank/topic, {@code null} when not found
 * @param currentStatus the status enum name, {@code null} when not found
 */
public record BulkUpdateQuestionScopeStatusFailure(
    UUID id,
    String code,
    String currentStatus,
    String reasonCode,
    String reason
) {
}
