package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkCriterionCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkCriterionInput;

public final class UpdateFrameworkCriterionCommandMapper {

    private UpdateFrameworkCriterionCommandMapper() {
    }

    public static UpdateFrameworkCriterionCommand fromInput(
            UUID frameworkId, UUID versionId, UUID criterionId, UpdateFrameworkCriterionInput input) {
        return new UpdateFrameworkCriterionCommand(
                frameworkId, versionId, criterionId,
                input.code(), input.name(), input.description(), input.order());
    }
}
