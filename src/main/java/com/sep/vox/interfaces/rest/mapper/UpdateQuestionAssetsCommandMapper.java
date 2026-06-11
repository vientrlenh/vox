package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetsRequest;

public final class UpdateQuestionAssetsCommandMapper {

    private UpdateQuestionAssetsCommandMapper() {
    }

    public static UpdateQuestionAssetsCommand fromRequest(UUID questionId, UpdateQuestionAssetsRequest request) {
        var assets = request.assets().stream()
            .map(a -> new UpdateQuestionAssetsCommand.AssetItem(
                a.title(), a.durationSeconds(), a.altText(), a.type(),
                a.url(), a.transcript(), a.description(), a.order()))
            .toList();
        return new UpdateQuestionAssetsCommand(questionId, assets);
    }
}
