package com.sep.vox.interfaces.graphql.input.gradelevel;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.gradelevel.UpdateGradeLevelCommand;

public record UpdateGradeLevelRequest(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<Integer> order
) {

    public static UpdateGradeLevelCommand toCommand(UUID gradeLevelId, UpdateGradeLevelRequest request) {
        return new UpdateGradeLevelCommand(
                gradeLevelId,
                request.name().value(),
                request.description().value(), !request.description().isOmitted(),
                request.order().value()
        );
    }
}
