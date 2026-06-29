package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record AcceptImportRequest(
        @NotEmpty(message = "Mapping import không được để trống")
        Map<String, String> confirmedMapping
) {
}