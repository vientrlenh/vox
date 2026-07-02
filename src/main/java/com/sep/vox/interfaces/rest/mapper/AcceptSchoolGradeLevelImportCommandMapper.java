package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolGradeLevelImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolGradeLevelImportRequest;

public final class AcceptSchoolGradeLevelImportCommandMapper {

    private AcceptSchoolGradeLevelImportCommandMapper() {
    }

    public static AcceptSchoolGradeLevelImportCommand fromRequest(UUID schoolId, UUID sessionId,
            AcceptSchoolGradeLevelImportRequest request) {
        return new AcceptSchoolGradeLevelImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
