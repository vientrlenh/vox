package com.sep.vox.application.response.input.practiceplanning;

import java.util.List;
import java.util.UUID;

public final class PracticePlanningResponses {

    private PracticePlanningResponses() {
    }

    public record PracticeTopicOffer(
            UUID topicId,
            String name,
            String dimension,
            boolean savedByMe,
            Integer matchPercent,
            int minutes,
            String level,
            String rationale,
            List<String> reasons,
            List<String> focusTags) {

        public PracticeTopicOffer(UUID topicId, String name, String dimension, boolean savedByMe) {
            this(topicId, name, dimension, savedByMe, null, 0, "INTERMEDIATE", null, List.of(), List.of());
        }
    }

    public record TopicSearchResult(
            List<PracticeTopicOffer> topics,
            boolean canGenerate) {
    }

    public record TopicInterest(
            UUID topicId,
            String name,
            double score,
            int sessionsMentioned,
            String lastMentionedAt) {
    }

    public record InterestProfile(List<TopicInterest> topics) {
    }

    public record PracticePaperQuestion(
            UUID questionId,
            int slot,
            String questionText,
            String criterionCode,
            String subAttribute,
            int difficultyRank,
            int preparationTimeSeconds,
            int maxResponseSeconds,
            int maxFollowupSeconds,
            List<String> suggestedIdeas) {
    }

    public record PracticePaper(
            UUID id,
            UUID topicId,
            String origin,
            int plannedSeconds,
            int reservedQuotaSeconds,
            List<PracticePaperQuestion> questions) {
    }
}
