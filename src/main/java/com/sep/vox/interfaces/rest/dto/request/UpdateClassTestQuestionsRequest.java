package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record UpdateClassTestQuestionsRequest(
    @NotEmpty(message = "Danh sách câu hỏi không được để trống")
    List<UUID> questionIds
) {
}
