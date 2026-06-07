package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

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
) {}