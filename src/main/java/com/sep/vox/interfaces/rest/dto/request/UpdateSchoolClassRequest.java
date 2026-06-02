package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSchoolClassRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2048) String description,
        @NotNull UUID targetSchoolLevelVersionId,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED") String status) {
}
