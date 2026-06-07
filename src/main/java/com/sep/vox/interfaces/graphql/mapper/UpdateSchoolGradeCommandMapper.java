package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeRequest;

import java.util.UUID;

public class UpdateSchoolGradeCommandMapper {
    public static UpdateSchoolGradeCommand fromRequest(UUID id, UpdateSchoolGradeRequest request) {
        return new UpdateSchoolGradeCommand(
                id,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}
