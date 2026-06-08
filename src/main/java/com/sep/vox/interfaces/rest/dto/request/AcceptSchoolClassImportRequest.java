package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolClassImportRequest(
    @NotEmpty(message = "Mapping import khong duoc de trong")
    Map<String, String> confirmedMapping
) {
}
