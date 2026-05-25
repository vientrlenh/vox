package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterSchoolAdminRequest(
    @NotBlank(message = "Email không được để trống")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Email(message = "Định dạng email không hợp lệ")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    String phone,

    @NotBlank(message = "Tên đầy đủ không được để trống")
    @Size(max = 255, message = "Tên đầy đủ không được để trống")
    String fullName,

    @NotBlank(message = "Ngày tháng năm sinh không được để trống")
    String dateOfBirth,

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 512, message = "Địa chỉ không được vượt quá 512 ký tự")
    String address,

    @NotNull(message = "Mã trường không được để trống")
    UUID schoolId
) {
    
}
