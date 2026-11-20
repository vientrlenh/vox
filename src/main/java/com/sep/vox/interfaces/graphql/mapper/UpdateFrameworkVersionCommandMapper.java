package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkVersionInput;

public final class UpdateFrameworkVersionCommandMapper {

    private UpdateFrameworkVersionCommandMapper() {
    }

    public static UpdateFrameworkVersionCommand fromInput(UUID frameworkId, UUID versionId, UpdateFrameworkVersionInput input) {
        var effectiveFrom = DateMapper.toInstant(input.effectiveFrom());
        var effectiveTo = DateMapper.toInstant(input.effectiveTo());
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
