package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricResultBandCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateRubricResultBandInput;

import java.util.UUID;

public class RubricResultBandGraphQLMapper {

    public static UpdateSystemRubricResultBandCommand fromSystemInput(UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSystemRubricResultBandCommand(
                resultBandId,
                input.code(),
                input.name(),
                input.description(),
                input.scoreMin(),
                input.scoreMax(),
                input.order()
        );
    }

    public static UpdateSchoolRubricResultBandCommand fromSchoolInput(UUID schoolId, UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSchoolRubricResultBandCommand(
                schoolId,
                resultBandId,
                input.code(),
                input.name(),
                input.description(),
                input.scoreMin(),
                input.scoreMax(),
                input.order()
        );
    }
}