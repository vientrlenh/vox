package com.sep.vox.domain.service.question;

/**
 * Status transition rule shared by question BANKS and question TOPICS.
 *
 * <p>The two entities have the same set of statuses ({@code DRAFT / PUBLISHED / ARCHIVED}) and the same rule, differing only in
 * the enum type. It takes the status NAME instead of the enum so one function serves both — in exchange
 * type checking is lost, so callers always pass {@code status.name()} and never build the string by hand.
 *
 * <p>A PURE function that returns the reason for rejection instead of throwing — mandatory for the bulk flow. If it threw,
 * the first rejected item would make Spring mark the transaction rollback-only and swallow the items
 * already updated successfully in earlier loop iterations; {@code BulkUpdateQuestionStatusUseCase} fell into exactly
 * this trap and its javadoc records it in full.
 */
public final class QuestionScopeStatusTransition {

    public enum RejectionCode {
        NOT_FOUND,
        NO_PERMISSION,
        INVALID_STATUS,
        INVALID_ACTION
    }

    public record Rejection(RejectionCode code, String reason) {
    }

    public static final String NOT_FOUND_BANK = "Question bank not found";
    public static final String NOT_FOUND_TOPIC = "Question topic not found";
    public static final String NO_PERMISSION = "You do not have permission to change the status of this item";

    private QuestionScopeStatusTransition() {
    }

    /**
     * @param currentStatusName {@code status.name()} of the item under consideration
     * @param action {@code PUBLISH} or {@code ARCHIVE}, already normalised to uppercase
     * @param label "question bank" / "question topic", only used to compose the message
     * @return {@code null} if the change is allowed
     */
    public static Rejection rejectionFor(String currentStatusName, String action, String label) {
        return switch (action == null ? "" : action) {
            case "PUBLISH" -> "DRAFT".equals(currentStatusName)
                ? null
                : new Rejection(
                    RejectionCode.INVALID_STATUS,
                    "Only a DRAFT " + label + " can be published (current: "
                        + currentStatusName + ")");
            // Can be archived from any status, matching exactly the ARCHIVE branch of the per-item change flow.
            case "ARCHIVE" -> null;
            default -> new Rejection(RejectionCode.INVALID_ACTION, "Invalid action");
        };
    }

    /** The status name after applying {@code action}. Only call when {@link #rejectionFor} returned null. */
    public static String nextStatusName(String action) {
        return "PUBLISH".equals(action) ? "PUBLISHED" : "ARCHIVED";
    }
}
