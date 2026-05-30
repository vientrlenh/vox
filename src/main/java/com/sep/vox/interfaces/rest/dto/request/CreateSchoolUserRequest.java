package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolUserRequest(
    @NotBlank(message = "Email không được để trống")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Email(message = "Email không hợp lệ")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    String phone,

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    String fullName,

    @NotBlank(message = "Ngày sinh không được để trống")
    String dateOfBirth,

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    String address,

    @NotBlank(message = "Vai trò không được để trống")
    String roleCode,

    @Size(max = 100, message = "Mã học sinh không được vượt quá 100 ký tự")
    String studentId
) {

}
