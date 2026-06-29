package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateRubricInput;

import java.util.UUID;

public class UpdateRubricGraphQLMapper {
    public static UpdateSystemRubricCommand fromSystemInput(UUID rubricId, UpdateRubricInput input) {
        return new UpdateSystemRubricCommand(
                rubricId,
                input.name(),
                input.description()
        );
    }
    public static UpdateSchoolRubricCommand fromSchoolInput(UUID schoolId, UUID rubricId, UpdateRubricInput input) {
        return new UpdateSchoolRubricCommand(
                schoolId,
                rubricId,
                input.name(),
                input.description()
        );
    }
}