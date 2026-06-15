package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionAssetsRequest(
    @NotNull(message = "Danh sách tài sản không được để trống")
    @Valid
    List<AssetItem> assets
) {
    public record AssetItem(
        UUID id,
        String title,
        Integer durationSeconds,
        String altText,
        @NotBlank(message = "Loại tài sản không được để trống") String type,
        String url,
        String transcript,
        String description,
        int order
    ) {
    }
}
