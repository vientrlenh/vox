package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;

public final class AcceptSchoolClassImportCommandMapper {

    private AcceptSchoolClassImportCommandMapper() {
    }

    public static AcceptSchoolClassImportCommand fromRequest(UUID schoolId, UUID sessionId, AcceptSchoolClassImportRequest request) {
        return new AcceptSchoolClassImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
