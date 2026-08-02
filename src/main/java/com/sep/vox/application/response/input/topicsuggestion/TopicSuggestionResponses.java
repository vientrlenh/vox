package com.sep.vox.application.response.input.topicsuggestion;

import java.util.UUID;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;

public final class TopicSuggestionResponses {

    private TopicSuggestionResponses() {
    }

    public record TopicSuggestion(
            UUID id,
            String suggestedTopicName,
            String interestDimension,
            double confidence,
            String reasonText,
            String status) {
    }

    public record TopicFromKeywordResult(
            PracticeTopicOffer topic,
            String outcome) {
    }
}
