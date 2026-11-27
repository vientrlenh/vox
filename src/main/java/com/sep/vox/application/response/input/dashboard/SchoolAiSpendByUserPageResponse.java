package com.sep.vox.application.response.input.dashboard;

import java.util.List;
import java.util.UUID;

/**
 * Who spent how much of the school's AI quota within the window being viewed.
 *
 * <p>{@code schoolWideCostVnd} is the part that does NOT belong to anyone -- centralized assessments are organized by the school, so
 * they are deliberately not counted against anyone's spending cap. Returned separately instead of being stuffed into an "unknown" row: in
 * almost every school it is the largest amount and would top the ranking, hiding exactly what this table
 * exists to show.
 *
 * <p>Without it the table total would never match the total of the chart just above, and the reader
 * would go looking for a leak that does not exist.
 */
public record SchoolAiSpendByUserPageResponse(
    List<UserAiSpendResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String schoolWideCostVnd
) {

    public record UserAiSpendResponse(
        UUID userId,
        String fullName,
        String quotaType,
        String spentVnd,
        /** The personal spending cap; null when the school has not divided a cap for this person. */
        String allocatedAmountVnd
    ) {
    }
}
