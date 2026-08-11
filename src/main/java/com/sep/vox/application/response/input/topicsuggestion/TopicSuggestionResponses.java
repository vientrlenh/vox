package com.sep.vox.application.response.input.topicsuggestion;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;

public final class TopicSuggestionResponses {

    private TopicSuggestionResponses() {
    }


    public record TopicFromKeywordResult(
            PracticeTopicOffer topic,
            String outcome) {
    }
}
