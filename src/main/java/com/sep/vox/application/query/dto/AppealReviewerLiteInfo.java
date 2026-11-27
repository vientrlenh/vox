package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * A candidate for the appeal-assignment picker.
 *
 * <p>People with a conflict of interest are NOT filtered out of the list but marked
 * {@code conflicted}: the admin needs to see them to understand why the choice is narrow, and in a
 * small school must also be able to override -- but must do so knowingly.
 *
 * @param load the number of open assignments this person currently holds, across all four rounds
 * @param conflicted has previously given a manual score for this very submission
 */
public record AppealReviewerLiteInfo(
    UUID id,
    String name,
    long load,
    boolean conflicted
) {
}
