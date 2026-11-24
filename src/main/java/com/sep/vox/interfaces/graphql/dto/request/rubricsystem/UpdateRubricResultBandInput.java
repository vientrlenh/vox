package com.sep.vox.interfaces.graphql.dto.request.rubricsystem;

import java.math.BigDecimal;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricResultBandCommand;
import java.util.UUID;

public record UpdateRubricResultBandInput(
        String name,
        String description,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        Integer order
) {

    public static UpdateSystemRubricResultBandCommand toUpdateSystemRubricResultBandCommand(UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSystemRubricResultBandCommand(
                resultBandId,
                input.name(),
                input.description(),
                input.scoreMin(),
                input.scoreMax(),
                input.order()
        );
    }

    public static UpdateSchoolRubricResultBandCommand toUpdateSchoolRubricResultBandCommand(UUID schoolId, UUID resultBandId, UpdateRubricResultBandInput input) {
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
