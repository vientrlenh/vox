package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionAssetDto(
    UUID id,
    UUID questionId,
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
