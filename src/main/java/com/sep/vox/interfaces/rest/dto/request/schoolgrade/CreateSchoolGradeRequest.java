package com.sep.vox.interfaces.rest.dto.request.schoolgrade;

import com.sep.vox.application.port.input.command.schoolgrade.CreateSchoolGradeCommand;
import java.util.UUID;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSchoolGradeRequest(

        @NotBlank(message = "Code must not be blank")
        String code,

        @NotBlank(message = "School year name must not be blank")
        String name,

        String description,

        @NotNull(message = "Start date must not be blank")
        LocalDate startDate,

        @NotNull(message = "End date must not be blank")
        LocalDate endDate
) {

    // Takes the grade level id as an extra parameter
    public static CreateSchoolGradeCommand toCommand(UUID schoolId, UUID gradeLevelId, CreateSchoolGradeRequest request) {
        return new CreateSchoolGradeCommand(
                schoolId,
                gradeLevelId, // Pass the grade level id through
                request.code(),
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}
