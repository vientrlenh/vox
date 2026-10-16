package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkCriterionCommand;

public record UpdateFrameworkCriterionInput(
    String code,
    String name,
    String description,
    int order
) {

    public static UpdateFrameworkCriterionCommand toCommand(UUID frameworkId, UUID versionId, UUID criterionId, UpdateFrameworkCriterionInput input) {
        return new UpdateFrameworkCriterionCommand(
                frameworkId, versionId, criterionId,
                input.code(), input.name(), input.description(), input.order());
    }
}
