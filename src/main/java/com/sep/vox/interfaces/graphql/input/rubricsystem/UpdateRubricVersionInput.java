package com.sep.vox.interfaces.graphql.input.rubricsystem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricVersionCommand;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

public record UpdateRubricVersionInput(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<Instant> effectiveFrom,
        ArgumentValue<Instant> effectiveTo,
        ArgumentValue<BigDecimal> scoringScaleMin,
        ArgumentValue<BigDecimal> scoringScaleMax,
        ArgumentValue<RubricTotalScoreMethod> totalScoreMethod
) {

    public static UpdateSystemRubricVersionCommand toUpdateSystemRubricVersionCommand(UUID versionId, UpdateRubricVersionInput input) {
        return new UpdateSystemRubricVersionCommand(
                versionId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted(),
                input.effectiveFrom().value(),
                input.effectiveTo().value(), !input.effectiveTo().isOmitted(),
                input.scoringScaleMin().value(),
                input.scoringScaleMax().value(),
                input.totalScoreMethod().value()
        );
    }

    public static UpdateSchoolRubricVersionCommand toUpdateSchoolRubricVersionCommand(UUID schoolId, UUID versionId, UpdateRubricVersionInput input) {
        return new UpdateSchoolRubricVersionCommand(
                schoolId,
                versionId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted(),
                input.effectiveFrom().value(),
                input.effectiveTo().value(), !input.effectiveTo().isOmitted(),
                input.scoringScaleMin().value(),
                input.scoringScaleMax().value(),
                input.totalScoreMethod().value()
        );
    }
}
