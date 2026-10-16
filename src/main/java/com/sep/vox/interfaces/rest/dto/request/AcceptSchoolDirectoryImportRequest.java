package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolDirectoryImportCommand;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolDirectoryImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolDirectoryImportCommand toCommand(UUID importSessionId, AcceptSchoolDirectoryImportRequest request) {
        return new AcceptSchoolDirectoryImportCommand(importSessionId, request.confirmedMapping);
    }
}
