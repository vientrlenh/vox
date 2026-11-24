package com.sep.vox.interfaces.graphql.input.schoolgrade;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.schoolgrade.UpdateSchoolGradeCommand;

public record UpdateSchoolGradeRequest(
        ArgumentValue<String> name,
        ArgumentValue<String> description,
        ArgumentValue<LocalDate> startDate,
        ArgumentValue<LocalDate> endDate
) {

    public static UpdateSchoolGradeCommand toCommand(UUID id, UpdateSchoolGradeRequest request) {
        return new UpdateSchoolGradeCommand(
                id,
                request.name().value(),
                request.description().value(), !request.description().isOmitted(),
                request.startDate().value(),
                request.endDate().value()
        );
    }
}
