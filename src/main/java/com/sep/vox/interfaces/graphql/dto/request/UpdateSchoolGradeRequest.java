package com.sep.vox.interfaces.graphql.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;

public record UpdateSchoolGradeRequest (
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
){

    public static UpdateSchoolGradeCommand toCommand(UUID id, UpdateSchoolGradeRequest request) {
        return new UpdateSchoolGradeCommand(
                id,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}
