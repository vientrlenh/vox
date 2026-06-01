package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddSchoolRoomRequest(
        @NotNull(message = "ID của trường học (schoolId) không được để trống")
        UUID schoolId,

        @NotBlank(message = "Mã phòng không được để trống")
        @Size(max = 50, message = "Mã phòng không được vượt quá 50 ký tự")
        String code,

        @NotBlank(message = "Tên phòng không được để trống")
        @Size(max = 100, message = "Tên phòng không được vượt quá 100 ký tự")
        String name,

        String description
) { }