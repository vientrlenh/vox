package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;

public final class AcceptSchoolUserImportCommandMapper {

    private AcceptSchoolUserImportCommandMapper() {
    }

    public static AcceptSchoolUserImportCommand fromRequest(UUID schoolId, UUID sessionId, AcceptSchoolUserImportRequest request) {
        return new AcceptSchoolUserImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
