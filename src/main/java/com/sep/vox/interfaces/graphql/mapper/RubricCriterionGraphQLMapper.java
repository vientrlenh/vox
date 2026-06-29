package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCriterionCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateRubricCriterionInput;

import java.util.UUID;

public class RubricCriterionGraphQLMapper {

    public static UpdateSystemRubricCriterionCommand fromSystemInput(UUID criterionId, UpdateRubricCriterionInput input) {
        return new UpdateSystemRubricCriterionCommand(
                criterionId,
                input.code(),
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
    public static UpdateSchoolRubricCriterionCommand fromSchoolInput(UUID schoolId, UUID criterionId, UpdateRubricCriterionInput input) {
        return new UpdateSchoolRubricCriterionCommand(
                schoolId,
                criterionId,
                input.code(),
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