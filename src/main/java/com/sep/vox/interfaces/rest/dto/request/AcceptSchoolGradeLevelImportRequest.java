package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolGradeLevelImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {
}
