package com.sep.vox.interfaces.graphql.input.framework;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkResultBandCommand;

public record UpdateFrameworkResultBandInput(
    String code,
    String label,
    ArgumentValue<String> description,
    int order
) {

    public static UpdateFrameworkResultBandCommand toCommand(
            UUID frameworkId, UUID versionId, UUID bandId, UpdateFrameworkResultBandInput input) {
        return new UpdateFrameworkResultBandCommand(
                frameworkId, versionId, bandId,
                input.code(), input.label(),
                input.description().value(), !input.description().isOmitted(), input.order());
    }
}
