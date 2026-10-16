package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCriterionCommand;

public record UpdateRubricCriterionInput(
        String name,
        String description,
        String examplesJson,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        Integer order,
        Boolean isRequired    // Dùng class Wrapper để hứng null
) {

    public static UpdateSystemRubricCriterionCommand toCommand(UUID criterionId, UpdateRubricCriterionInput input) {
        return new UpdateSystemRubricCriterionCommand(
                criterionId,
                input.name(),
                input.description(),
                input.examplesJson(),
                input.weight(),
                input.minScore(),
                input.maxScore(),
                input.order(),
                input.isRequired()
        );
    }

    public static UpdateSchoolRubricCriterionCommand toCommand(UUID schoolId, UUID criterionId, UpdateRubricCriterionInput input) {
        return new UpdateSchoolRubricCriterionCommand(
                schoolId,
                criterionId,
                input.name(),
                input.description(),
                input.examplesJson(),
                input.weight(),
                input.minScore(),
                input.maxScore(),
                input.order(),
                input.isRequired()
        );
    }
}
