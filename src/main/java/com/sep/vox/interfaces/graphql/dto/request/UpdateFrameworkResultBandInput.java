package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkResultBandCommand;

public record UpdateFrameworkResultBandInput(
    String code,
    String label,
    String description,
    int order
) {

    public static UpdateFrameworkResultBandCommand toCommand(UUID frameworkId, UUID versionId, UUID bandId, UpdateFrameworkResultBandInput input) {
        return new UpdateFrameworkResultBandCommand(
                frameworkId, versionId, bandId,
                input.code(), input.label(), input.description(), input.order());
    }
}
