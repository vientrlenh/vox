package com.sep.vox.interfaces.graphql.input.schoolclass;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.schoolclass.UpdateSchoolClassCommand;
import com.sep.vox.domain.model.school.SchoolClassStatus;

public record UpdateSchoolClassInput(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<SchoolClassStatus> status) {

    public static UpdateSchoolClassCommand toCommand(UUID id, UpdateSchoolClassInput input) {
        return new UpdateSchoolClassCommand(
                id,
                input.name().value(), !input.name().isOmitted(),
                input.description().value(), !input.description().isOmitted(),
                input.status().value(), !input.status().isOmitted());
    }
}
