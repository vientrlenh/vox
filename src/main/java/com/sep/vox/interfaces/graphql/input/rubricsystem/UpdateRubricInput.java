package com.sep.vox.interfaces.graphql.input.rubricsystem;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCommand;

public record UpdateRubricInput(
        ArgumentValue<String> name,
        ArgumentValue<String> description
) {

    public static UpdateSystemRubricCommand toUpdateSystemRubricCommand(UUID rubricId, UpdateRubricInput input) {
        return new UpdateSystemRubricCommand(
                rubricId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted()
        );
    }

    public static UpdateSchoolRubricCommand toUpdateSchoolRubricCommand(UUID schoolId, UUID rubricId, UpdateRubricInput input) {
        return new UpdateSchoolRubricCommand(
                schoolId,
                rubricId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted()
        );
    }
}
