package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record UpdateQuestionAssetsCommand(
    UUID questionId,
    List<AssetItem> assets
) {
    public record AssetItem(
        UUID id,
        String title,
        Integer durationSeconds,
        String altText,
        String type,
        String url,
        String transcript,
        String description,
        int order
    ) {
    }
}
