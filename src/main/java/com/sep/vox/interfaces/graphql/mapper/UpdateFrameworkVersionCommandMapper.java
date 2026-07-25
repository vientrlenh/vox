package com.sep.vox.interfaces.graphql.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkVersionInput;

public final class UpdateFrameworkVersionCommandMapper {

    private UpdateFrameworkVersionCommandMapper() {
    }

    public static UpdateFrameworkVersionCommand fromInput(UUID frameworkId, UUID versionId, UpdateFrameworkVersionInput input) {
        var effectiveFrom = parseDateTime(input.effectiveFrom());
        var effectiveTo = parseDateTime(input.effectiveTo());
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

    private static OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }
}
