package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptSchoolGradeImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolGradeImportRequest;

public final class AcceptSchoolGradeImportCommandMapper {

    private AcceptSchoolGradeImportCommandMapper() {
    }

    public static AcceptSchoolGradeImportCommand fromRequest(UUID schoolId, UUID sessionId, AcceptSchoolGradeImportRequest request) {
        return new AcceptSchoolGradeImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
