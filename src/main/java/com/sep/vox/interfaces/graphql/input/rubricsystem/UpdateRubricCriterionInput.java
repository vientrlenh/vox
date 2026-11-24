package com.sep.vox.interfaces.graphql.input.rubricsystem;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCriterionCommand;

public record UpdateRubricCriterionInput(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<String> examplesJson,
        ArgumentValue<BigDecimal> weight,
        ArgumentValue<BigDecimal> minScore,
        ArgumentValue<BigDecimal> maxScore,
        ArgumentValue<Integer> order,
        ArgumentValue<Boolean> isRequired
) {

    public static UpdateSystemRubricCriterionCommand toUpdateSystemRubricCriterionCommand(UUID criterionId, UpdateRubricCriterionInput input) {
        return new UpdateSystemRubricCriterionCommand(
                criterionId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted(),
                input.examplesJson().value(), !input.examplesJson().isOmitted(),
                input.weight().value(),
                input.minScore().value(),
                input.maxScore().value(),
                input.order().value(),
                input.isRequired().value()
        );
    }

    public static UpdateSchoolRubricCriterionCommand toUpdateSchoolRubricCriterionCommand(UUID schoolId, UUID criterionId, UpdateRubricCriterionInput input) {
        return new UpdateSchoolRubricCriterionCommand(
                schoolId,
                criterionId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted(),
                input.examplesJson().value(), !input.examplesJson().isOmitted(),
                input.weight().value(),
                input.minScore().value(),
                input.maxScore().value(),
                input.order().value(),
                input.isRequired().value()
        );
    }
}
