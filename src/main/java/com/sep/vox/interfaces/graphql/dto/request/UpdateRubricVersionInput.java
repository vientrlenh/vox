package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricVersionCommand;

public record UpdateRubricVersionInput(
        String name,
        String description,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        String totalScoreMethod
) {

    public static UpdateSystemRubricVersionCommand toCommand(UUID versionId, UpdateRubricVersionInput input) {
        return new UpdateSystemRubricVersionCommand(
                versionId,
                input.name(),
                input.description(),
                input.effectiveFrom(),
                input.effectiveTo(),
                input.scoringScaleMin(),
                input.scoringScaleMax(),
                input.totalScoreMethod()
        );
    }

    public static UpdateSchoolRubricVersionCommand toCommand(UUID schoolId, UUID versionId, UpdateRubricVersionInput input) {
        return new UpdateSchoolRubricVersionCommand(
                schoolId,
                versionId,
                input.name(),
                input.description(),
                input.effectiveFrom(),
                input.effectiveTo(),
                input.scoringScaleMin(),
                input.scoringScaleMax(),
                input.totalScoreMethod()
        );
    }
}
