package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionAssetMutationCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionAssetRequest;

public final class CreateQuestionAssetCommandMapper {

    private CreateQuestionAssetCommandMapper() {
    }

    public static CreateQuestionAssetMutationCommand fromRequest(UUID questionId, CreateQuestionAssetRequest request) {
        return new CreateQuestionAssetMutationCommand(
            questionId,
            request.title(),
            request.durationSeconds(),
            request.altText(),
            request.type(),
            request.url(),
            request.transcript(),
            request.description(),
            request.order()
        );
    }
}
