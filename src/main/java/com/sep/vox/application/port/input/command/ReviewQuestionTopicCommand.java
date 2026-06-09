package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionTopicStatus;

public record ReviewQuestionTopicCommand(
    UUID topicId,
    QuestionTopicStatus targetStatus
) {
}
