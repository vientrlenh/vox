package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

public record QuestionSelectionSpecRequest(
    String questionType,
    String difficulty,
    String targetBandLevel,
    String skillCode,
    UUID topicId
) {
}
