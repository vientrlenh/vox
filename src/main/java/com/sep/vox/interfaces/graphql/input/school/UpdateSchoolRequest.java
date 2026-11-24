package com.sep.vox.interfaces.graphql.input.school;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.school.UpdateSchoolCommand;

public record UpdateSchoolRequest(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<String> contactPhone,
        ArgumentValue<String> contactEmail,
        ArgumentValue<String> domain,
        ArgumentValue<String> address,
        ArgumentValue<Integer> studentCount
) {

    public static UpdateSchoolCommand toCommand(UUID id, UpdateSchoolRequest request) {
        return new UpdateSchoolCommand(
                id,
                request.name().value(),
                request.description().value(), !request.description().isOmitted(),
                request.contactPhone().value(),
                request.contactEmail().value(),
                request.domain().value(), !request.domain().isOmitted(),
                request.address().value(),
                request.studentCount().value()
        );
    }
}
