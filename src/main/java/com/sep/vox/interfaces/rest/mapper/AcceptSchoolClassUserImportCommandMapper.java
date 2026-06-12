package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;

public final class AcceptSchoolClassUserImportCommandMapper {

    private AcceptSchoolClassUserImportCommandMapper() {
    }

    public static AcceptSchoolClassUserImportCommand fromRequest(
            UUID schoolId,
            UUID sessionId,
            AcceptSchoolClassUserImportRequest request) {
        return new AcceptSchoolClassUserImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
