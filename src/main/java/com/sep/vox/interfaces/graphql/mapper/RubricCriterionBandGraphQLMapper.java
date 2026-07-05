package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricCriterionBandCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCriterionBandCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateRubricCriterionBandInput;

import java.util.UUID;

public class RubricCriterionBandGraphQLMapper {
    public static UpdateSchoolRubricCriterionBandCommand fromSchoolInput(UUID schoolId, UUID bandId, UpdateRubricCriterionBandInput input) {
        return new UpdateSchoolRubricCriterionBandCommand(
                schoolId,
                bandId,
                input.scoreMin(),
                input.scoreMax()
        );
    }

    public static UpdateSystemRubricCriterionBandCommand fromSystemInput(UUID bandId, UpdateRubricCriterionBandInput input) {
        return new UpdateSystemRubricCriterionBandCommand(
                bandId,
                input.scoreMin(),
                input.scoreMax()
        );
    }
}
