package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricResultBandCommand;

public record UpdateRubricResultBandInput(
        String name,
        String description,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        Integer order
) {

    public static UpdateSystemRubricResultBandCommand toCommand(UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSystemRubricResultBandCommand(
                resultBandId,
                input.name(),
                input.description(),
                input.scoreMin(),
                input.scoreMax(),
                input.order()
        );
    }

    public static UpdateSchoolRubricResultBandCommand toCommand(UUID schoolId, UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSchoolRubricResultBandCommand(
                schoolId,
                resultBandId,
                input.name(),
                input.description(),
                input.scoreMin(),
                input.scoreMax(),
                input.order()
        );
    }
}
