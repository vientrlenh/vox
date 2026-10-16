package com.sep.vox.interfaces.graphql.dto.request;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;

public record UpdateFrameworkVersionInput(
    String code,
    String name,
    String description,
    Instant effectiveFrom,
    Instant effectiveTo
) {

    public static UpdateFrameworkVersionCommand toCommand(UUID frameworkId, UUID versionId, UpdateFrameworkVersionInput input) {
        var effectiveFrom = input.effectiveFrom();
        var effectiveTo = input.effectiveTo();
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("Ngày kết thúc hiệu lực phải sau ngày bắt đầu hiệu lực");

        return new UpdateFrameworkVersionCommand(
            frameworkId,
            versionId,
            input.code(),
            input.name(),
            input.description(),
            effectiveFrom,
            effectiveTo
        );
    }
}
