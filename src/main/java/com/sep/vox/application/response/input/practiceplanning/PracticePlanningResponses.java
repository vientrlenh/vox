package com.sep.vox.application.response.input.practiceplanning;

import java.util.List;
import java.util.UUID;

public final class PracticePlanningResponses {

    private PracticePlanningResponses() {
    }

    /**
     * The {@code level} field is gone. Before, each card carried its own level, so a student
     * saw "Technology — Advanced" next to "Food — Basic" and clicked the second: the system recorded
     * "the student likes Food" when they had only chosen the easier one. Now the difficulty is chosen ONCE for the whole
     * session so every card has the same level, and topic selection is purely about interest again.
     */
    public record PracticeTopicOffer(
            UUID topicId,
            String name,
            String dimension,
            boolean savedByMe,
            Integer matchPercent,
            int minutes,
            String rationale,
            List<String> reasons) {

        public PracticeTopicOffer(UUID topicId, String name, String dimension, boolean savedByMe) {
            this(topicId, name, dimension, savedByMe, null, 0, null, List.of());
        }
    }

    public record TopicSearchResult(
            List<PracticeTopicOffer> topics,
            boolean canGenerate) {
    }

    public record PracticePaperQuestion(
            UUID questionId,
            int slot,
            String questionText,
            String criterionCode,
            String subAttribute,
            int difficultyRank,
            int maxResponseSeconds,
            int minResponseSeconds,
            List<String> suggestedIdeas) {
    }

    public record PracticePaper(
            UUID id,
            UUID topicId,
            String origin,
            int plannedSeconds,
            int reservedQuotaSeconds,
            /** The speaking cap of the whole session (seconds) -- see PracticePaperDto.sessionBudgetSeconds. */
            int sessionBudgetSeconds,
            List<PracticePaperQuestion> questions) {
    }

    /**
     * The result of building a paper in the 2-phase model: building a paper may need the AI to generate new questions
     * (10-20s) when the bank has no suitable questions, too long to hold one HTTP request. Phase 1
     * (buildPracticePaper) returns READY immediately if the bank already has them, or PREPARING if they are being
     * generated; phase 2 (practicePaperDraft) is for the client to ask again.
     */
    public record PracticePaperDraft(
            UUID draftId,
            String status,
            String reason,
            PracticePaper paper,
            /** Only present when status=FAILED. A machine-readable error code (for example BUSINESS_RULE_VIOLATION) so
             * the client can tell a business error (not worth retrying) from a temporary one (network loss, AI
             * generation failure). */
            String errorCode) {

        public static PracticePaperDraft preparing(UUID draftId) {
            return new PracticePaperDraft(draftId, "PREPARING", null, null, null);
        }

        public static PracticePaperDraft ready(UUID draftId, PracticePaper paper) {
            return new PracticePaperDraft(draftId, "READY", null, paper, null);
        }

        public static PracticePaperDraft failed(UUID draftId, String reason, String errorCode) {
            return new PracticePaperDraft(draftId, "FAILED", reason, null, errorCode);
        }
    }
}
