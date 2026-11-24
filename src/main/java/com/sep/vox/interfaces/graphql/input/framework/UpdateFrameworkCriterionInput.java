package com.sep.vox.interfaces.graphql.input.framework;

import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkCriterionCommand;

public record UpdateFrameworkCriterionInput(
    String code,
    String name,
    ArgumentValue<String> description,
    int order
) {

    public static UpdateFrameworkCriterionCommand toCommand(
            UUID frameworkId, UUID versionId, UUID criterionId, UpdateFrameworkCriterionInput input) {
        return new UpdateFrameworkCriterionCommand(
                frameworkId, versionId, criterionId,
                input.code(), input.name(),
                input.description().value(), !input.description().isOmitted(), input.order());
    }
}
