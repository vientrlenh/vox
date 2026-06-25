package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolDirectoryImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolDirectoryImportRequest;

public final class AcceptSchoolDirectoryImportCommandMapper {

    private AcceptSchoolDirectoryImportCommandMapper() {
    }

    public static AcceptSchoolDirectoryImportCommand fromRequest(UUID sessionId, AcceptSchoolDirectoryImportRequest request) {
        return new AcceptSchoolDirectoryImportCommand(sessionId, request.confirmedMapping());
    }
}
