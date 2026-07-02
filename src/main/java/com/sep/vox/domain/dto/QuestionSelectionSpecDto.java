package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionSelectionSpecDto(
    String questionType,
    String difficulty,
    String targetBandLevel,
    String skillCode,
    UUID topicId
) {
}
