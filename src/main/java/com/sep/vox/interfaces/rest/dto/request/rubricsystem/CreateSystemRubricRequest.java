package com.sep.vox.interfaces.rest.dto.request.rubricsystem;


import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricCommand;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateSystemRubricRequest(
        @NotBlank(message = "Mã bộ tiêu chí không được để trống")
        @Size(max = 50, message = "Mã bộ tiêu chí tối đa 50 ký tự")
        String code,

        @NotBlank(message = "Tên bộ tiêu chí không được để trống")
        @Size(max = 255, message = "Tên bộ tiêu chí tối đa 255 ký tự")
        String name,

        @Size(max = 2048, message = "Mô tả tối đa 2048 ký tự")
        String description,

        @NotNull(message = "Ngôn ngữ không được để trống")
        UUID languageId,

        @NotNull(message = "Khung tiêu chuẩn (Framework) không được để trống")
        UUID frameworkId
) {

    public static CreateSystemRubricCommand toCommand(CreateSystemRubricRequest request) {
        return new CreateSystemRubricCommand(
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId()
        );
    }
}
