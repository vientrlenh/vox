package com.sep.vox.interfaces.graphql.dto.request.rubricsystem;

import java.math.BigDecimal;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCriterionCommand;
import java.util.UUID;

public record UpdateRubricCriterionInput(
        String name,
        String description,
        String examplesJson,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        Integer order,
        Boolean isRequired    // Wrapper class so that null can be received
) {

    public static UpdateSystemRubricCriterionCommand toUpdateSystemRubricCriterionCommand(UUID criterionId, UpdateRubricCriterionInput input) {
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

    public static UpdateSchoolRubricCriterionCommand toUpdateSchoolRubricCriterionCommand(UUID schoolId, UUID criterionId, UpdateRubricCriterionInput input) {
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
