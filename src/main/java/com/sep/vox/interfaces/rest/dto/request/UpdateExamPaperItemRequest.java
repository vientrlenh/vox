package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateExamPaperItemRequest(
    @NotNull(message = "QuestionId là bắt buộc")
    UUID questionId
) {
}
