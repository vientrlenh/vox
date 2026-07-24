package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkResultBandCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkResultBandInput;

public final class UpdateFrameworkResultBandCommandMapper {

    private UpdateFrameworkResultBandCommandMapper() {
    }

    public static UpdateFrameworkResultBandCommand fromInput(
            UUID frameworkId, UUID versionId, UUID bandId, UpdateFrameworkResultBandInput input) {
        return new UpdateFrameworkResultBandCommand(
                frameworkId, versionId, bandId,
                input.code(), input.label(), input.description(), input.order());
    }
}
