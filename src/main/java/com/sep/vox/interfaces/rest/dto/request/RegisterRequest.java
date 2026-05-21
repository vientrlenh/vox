package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
    @NotBlank(message = "Tên liên hệ không được để trống")
    String contactFullName,

    @NotBlank(message = "Mã định danh không được để trống")
    String identityNumber,

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    String contactPhone,

    @NotBlank(message = "Email liên hệ không được để trống")
    String contactEmail,

    @NotBlank(message = "Tên miền của trường không được để trống")
    String schoolDomain,

    @NotBlank(message = "Tên trường không được để trống")
    String schoolName,

    @NotBlank(message = "Địa chỉ trường không được để trống")
    String schoolAddress,

    @NotBlank(message = "Mã bưu chính không được để trống")
    String postalCode,

    @NotBlank(message = "Chức vụ không được để trống")
    String position,

    @NotNull(message = "Số học sinh không được để trống")
    int studentCount
) {
    
}
