package com.sep.vox.interfaces.graphql.dto.request.rubricsystem;

import java.math.BigDecimal;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import java.time.Instant;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricVersionCommand;
import java.util.UUID;

public record UpdateRubricVersionInput(
        String name,
        String description,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        RubricTotalScoreMethod totalScoreMethod
) {

    public static UpdateSystemRubricVersionCommand toUpdateSystemRubricVersionCommand(UUID versionId, UpdateRubricVersionInput input) {
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

    public static UpdateSchoolRubricVersionCommand toUpdateSchoolRubricVersionCommand(UUID schoolId, UUID versionId, UpdateRubricVersionInput input) {
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
