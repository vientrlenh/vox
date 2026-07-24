package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkCriteriaRequest(
        @NotEmpty(message = "Phải gửi lên ít nhất 1 tiêu chí")
        @Valid
        List<CriterionItemRequest> criteria
) {
    public record CriterionItemRequest(
            @NotBlank(message = "Mã tiêu chí không được để trống")
            String code,

            @NotBlank(message = "Tên tiêu chí không được để trống")
            String name,

            String description,

            int order
    ) {}
}
