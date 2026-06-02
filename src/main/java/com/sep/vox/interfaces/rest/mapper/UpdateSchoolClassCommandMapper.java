package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassRequest;

public final class UpdateSchoolClassCommandMapper {

    private UpdateSchoolClassCommandMapper() {
    }

    public static UpdateSchoolClassCommand fromRequest(UUID id, UpdateSchoolClassRequest request) {
        return new UpdateSchoolClassCommand(
            id,
            request.name(),
            request.description(),
            request.targetSchoolLevelVersionId(),
            request.status()
        );
    }
}
