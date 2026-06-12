package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricApplicabilityRequest(
        @NotEmpty(message = "Phải cung cấp ít nhất 1 phạm vi áp dụng (Khối hoặc Lớp).")
        @Valid
        List<ApplicabilityItemRequest> applicabilities
) {
    public record ApplicabilityItemRequest(
            @NotNull(message = "ID khối học không được để trống")
            UUID schoolGradeId,
            @NotNull(message = "ID lớp học không được để trống")
            UUID schoolClassId,
            @NotBlank(message = "Ngày bắt đầu áp dụng không được để trống")
            String effectiveFrom,
            String effectiveTo
    ) {}
}