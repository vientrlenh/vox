package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
    @NotNull(message = "ID ngôn ngữ không được để trống")
    UUID languageId,

    @NotNull(message = "ID khối học không được để trống")
    UUID schoolGradeId,

    @NotBlank(message = "Mã lớp học không được để trống")
    @Size(max = 100, message = "Mã lớp học không được vượt quá 100 ký tự")
    String code,

    @NotBlank(message = "Tên lớp học không được để trống")
    @Size(max = 255, message = "Tên lớp học không được vượt quá 255 ký tự")
    String name,

    @Size(max = 2048, message = "Mô tả không được vượt quá 2048 ký tự")
    String description,

    @NotNull(message = "ID phiên bản cấp độ mục tiêu không được để trống")
    UUID targetSchoolLevelVersionId
) {
}
