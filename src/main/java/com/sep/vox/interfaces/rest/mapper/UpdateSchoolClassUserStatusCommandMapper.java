package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassUserStatusRequest;

public final class UpdateSchoolClassUserStatusCommandMapper {

    private UpdateSchoolClassUserStatusCommandMapper() {
    }

    public static UpdateSchoolClassUserStatusCommand fromRequest(
            UUID schoolId,
            UUID classId,
            UUID userId,
            UpdateSchoolClassUserStatusRequest request) {
        return new UpdateSchoolClassUserStatusCommand(schoolId, classId, userId, request.isActive());
    }
}
