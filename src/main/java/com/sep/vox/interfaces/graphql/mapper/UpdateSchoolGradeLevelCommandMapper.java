package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolGradeLevelCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeLevelRequest;

import java.util.UUID;

public final class UpdateSchoolGradeLevelCommandMapper {

    private UpdateSchoolGradeLevelCommandMapper() {
    }

    public static UpdateSchoolGradeLevelCommand fromRequest(UUID schoolId, UUID gradeLevelId,
            UpdateSchoolGradeLevelRequest request) {
        return new UpdateSchoolGradeLevelCommand(
                schoolId,
                gradeLevelId,
                request.name(),
                request.description(),
                request.order()
        );
    }
}
