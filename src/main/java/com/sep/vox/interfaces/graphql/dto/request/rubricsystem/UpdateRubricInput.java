package com.sep.vox.interfaces.graphql.dto.request.rubricsystem;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCommand;
import java.util.UUID;

public record UpdateRubricInput(
        String name,
        String description
) {

    public static UpdateSystemRubricCommand toUpdateSystemRubricCommand(UUID rubricId, UpdateRubricInput input) {
        return new UpdateSystemRubricCommand(
                rubricId,
                input.name(),
                input.description()
        );
    }

    public static UpdateSchoolRubricCommand toUpdateSchoolRubricCommand(UUID schoolId, UUID rubricId, UpdateRubricInput input) {
        return new UpdateSchoolRubricCommand(
                schoolId,
                rubricId,
                input.name(),
                input.description()
        );
    }
}
