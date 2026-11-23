package com.sep.vox.interfaces.rest.dto.request.rubricschool;

import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSchoolRubricRequest(
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

    public static CreateSchoolRubricCommand toCommand(UUID schoolId, CreateSchoolRubricRequest request) {
        return new CreateSchoolRubricCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId()
        );
    }
}
