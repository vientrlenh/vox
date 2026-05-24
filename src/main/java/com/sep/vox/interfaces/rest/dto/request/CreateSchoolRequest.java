package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolRequest(
    @NotBlank(message = "Mã trường không được để trống")
    @Size(max = 100, message = "Mã trường không được vượt quá 100 ký tự")
    String code,

    @NotBlank(message = "Tên trường không được để trống")
    @Size(max = 255, message = "Tên trường không được vượt quá 255 ký tự")
    String name,

    @Size(max = 2048, message = "Mô tả trường không được vượt quá 2048 ký tự")
    String description,

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    @Size(max = 20, message = "Số điện thoại liên hệ không được vượt quá 20 ký tự")
    String contactPhone,

    @NotBlank(message = "Email liên hệ không được để trống")
    @Size(max = 255, message = "Email liên hệ không được vượt quá 255 ký tự")
    @Email(message = "Email không hợp lệ")
    String contactEmail,

    @NotBlank(message = "Tên miền của trường không được để trống")
    @Size(max = 100, message = "Tên miền của trường không được vượt quá 100 ký tự")
    String domain,

    @NotBlank(message = "Địa chỉ của trường không được để trống")
    @Size(max = 512, message = "Địa chỉ của trường không được vượt quá 512 ký tự")
    String address,

    @NotNull(message = "Số lượng học sinh của trường không được để trống")
    @Min(value = 1, message = "Số lượng học sinh không được dưới 1")
    int studentCount
) {
    
}
