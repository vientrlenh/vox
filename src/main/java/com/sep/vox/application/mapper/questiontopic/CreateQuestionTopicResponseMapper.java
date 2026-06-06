package com.sep.vox.application.mapper.questiontopic;

import java.util.UUID;

import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;

public final class CreateQuestionTopicResponseMapper {
    

    public static CreateQuestionTopicResponse toResponse(UUID questionTopicId) {
        return new CreateQuestionTopicResponse(questionTopicId);
    }
}
