package com.sep.vox.interfaces.graphql.input.rubricsystem;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricResultBandCommand;

public record UpdateRubricResultBandInput(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<BigDecimal> scoreMin,
        ArgumentValue<BigDecimal> scoreMax,
        ArgumentValue<Integer> order
) {

    public static UpdateSystemRubricResultBandCommand toUpdateSystemRubricResultBandCommand(UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSystemRubricResultBandCommand(
                resultBandId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted(),
                input.scoreMin().value(),
                input.scoreMax().value(),
                input.order().value()
        );
    }

    public static UpdateSchoolRubricResultBandCommand toUpdateSchoolRubricResultBandCommand(UUID schoolId, UUID resultBandId, UpdateRubricResultBandInput input) {
        return new UpdateSchoolRubricResultBandCommand(
                schoolId,
                resultBandId,
                input.name().value(),
                input.description().value(), !input.description().isOmitted(),
                input.scoreMin().value(),
                input.scoreMax().value(),
                input.order().value()
        );
    }
}
