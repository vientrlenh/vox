package com.sep.vox.interfaces.graphql.input.framework;

import java.time.Instant;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkVersionCommand;

public record UpdateFrameworkVersionInput(
    String code,
    String name,
    ArgumentValue<String> description,
    Instant effectiveFrom,
    ArgumentValue<Instant> effectiveTo
) {

    public static UpdateFrameworkVersionCommand toCommand(UUID frameworkId, UUID versionId, UpdateFrameworkVersionInput input) {
        Instant effectiveTo = input.effectiveTo().value();
        if (input.effectiveFrom() != null && effectiveTo != null && effectiveTo.isBefore(input.effectiveFrom()))
            throw new IllegalArgumentException("The effective end date must be after the effective start date");

        return new UpdateFrameworkVersionCommand(
            frameworkId,
            versionId,
            input.code(),
            input.name(),
            input.description().value(), !input.description().isOmitted(),
            input.effectiveFrom(),
            effectiveTo, !input.effectiveTo().isOmitted()
        );
    }
}
