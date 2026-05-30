package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SchoolUserImportRequest(
    @NotBlank(message = "FileId không được để trống")
    String fileId,

    boolean dryRun,

    @Size(max = 50, message = "Vai trò mặc định không được vượt quá 50 ký tự")
    String defaultRole,

    @NotNull(message = "Mapping không được để trống")
    Map<String, ImportFieldMappingRequest> mapping
) {
}
