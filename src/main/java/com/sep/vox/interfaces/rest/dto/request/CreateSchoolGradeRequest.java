package com.sep.vox.interfaces.rest.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSchoolGradeRequest(

        @NotBlank(message = "Mã code không được để trống")
        String code,

        @NotBlank(message = "Tên năm học không được để trống")
        String name,

        String description,

        @NotNull(message = "Ngày bắt đầu không được để trống")
        LocalDate startDate,

        @NotNull(message = "Ngày kết thúc không được để trống")
        LocalDate endDate
) {

    public static CreateSchoolGradeCommand toCommand(UUID schoolId, UUID gradeLevelId, CreateSchoolGradeRequest request) {
        return new CreateSchoolGradeCommand(
                schoolId,
                gradeLevelId,
                request.code(),
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}
