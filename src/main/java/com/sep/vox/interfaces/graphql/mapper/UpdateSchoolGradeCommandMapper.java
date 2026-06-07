package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeRequest;

import java.util.UUID;

public class UpdateSchoolGradeCommandMapper {
    public static UpdateSchoolGradeCommand fromRequest(UpdateSchoolGradeRequest request) {
        return new UpdateSchoolGradeCommand(
                request.schoolGradeId(),
                request.schoolId(),
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}
