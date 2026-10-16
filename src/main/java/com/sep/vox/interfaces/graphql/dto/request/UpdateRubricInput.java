package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCommand;

public record UpdateRubricInput(
        String name,
        String description
) {

    public static UpdateSystemRubricCommand toCommand(UUID rubricId, UpdateRubricInput input) {
        return new UpdateSystemRubricCommand(
                rubricId,
                input.name(),
                input.description()
        );
    }

    public static UpdateSchoolRubricCommand toCommand(UUID schoolId, UUID rubricId, UpdateRubricInput input) {
        return new UpdateSchoolRubricCommand(
                schoolId,
                rubricId,
                input.name(),
                input.description()
        );
    }
}
