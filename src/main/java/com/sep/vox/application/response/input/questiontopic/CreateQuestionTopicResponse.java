package com.sep.vox.application.response.input.questiontopic;

import java.util.UUID;

public record CreateQuestionTopicResponse(
    UUID questionTopicId
) {

    public static CreateQuestionTopicResponse toResponse(UUID questionTopicId) {
        return new CreateQuestionTopicResponse(questionTopicId);
    }
}
