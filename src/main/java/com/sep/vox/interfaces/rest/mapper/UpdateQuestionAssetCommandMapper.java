package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionAssetCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetRequest;

public final class UpdateQuestionAssetCommandMapper {

    private UpdateQuestionAssetCommandMapper() {
    }

    public static UpdateQuestionAssetCommand fromRequest(
            UUID questionId,
            UUID assetId,
            UpdateQuestionAssetRequest request) {
        return new UpdateQuestionAssetCommand(
            questionId,
            assetId,
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
