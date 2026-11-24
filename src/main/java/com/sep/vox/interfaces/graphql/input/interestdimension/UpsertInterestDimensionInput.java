package com.sep.vox.interfaces.graphql.input.interestdimension;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.interestdimension.UpsertInterestDimensionCommand;

public record UpsertInterestDimensionInput(
    String code,
    ArgumentValue<String> label,
    ArgumentValue<String> description,
    ArgumentValue<Boolean> active,
    ArgumentValue<Boolean> quizEligible,
    ArgumentValue<Integer> displayOrder) {

    public static UpsertInterestDimensionCommand toCommand(UpsertInterestDimensionInput input) {
        return new UpsertInterestDimensionCommand(
            input.code(),
            input.label().value(),
            input.description().value(), !input.description().isOmitted(),
            input.active().value(),
            input.quizEligible().value(),
            input.displayOrder().value()
        );
    }
}
