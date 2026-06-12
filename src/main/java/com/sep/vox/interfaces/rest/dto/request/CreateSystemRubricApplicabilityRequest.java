package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSystemRubricApplicabilityRequest(
        @NotEmpty(message = "Phải cung cấp thời gian áp dụng.")
        @Valid
        List<ApplicabilityItemRequest> applicabilities
) {
    public record ApplicabilityItemRequest(
            @NotBlank(message = "Ngày bắt đầu áp dụng không được để trống")
            String effectiveFrom,

            String effectiveTo
    ) {}
}