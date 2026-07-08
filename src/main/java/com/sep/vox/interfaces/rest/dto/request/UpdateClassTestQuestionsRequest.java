package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateClassTestQuestionsRequest(
    @Valid
    @NotEmpty(message = "Danh sách section không được để trống")
    List<ClassTestSectionRequest> sections
) {
}
