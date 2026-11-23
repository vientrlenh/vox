package com.sep.vox.interfaces.rest.dto.request.schooldirectory;

import com.sep.vox.application.port.input.command.schooldirectory.AcceptSchoolDirectoryImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptSchoolDirectoryImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSchoolDirectoryImportCommand toCommand(UUID sessionId, AcceptSchoolDirectoryImportRequest request) {
        return new AcceptSchoolDirectoryImportCommand(sessionId, request.confirmedMapping());
    }
}
