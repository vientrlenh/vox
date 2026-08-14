package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolRequest(
    UUID schoolDirectoryId, 

    @Size(max = 100, message = "Mã trường không được vượt quá 100 ký tự")
    String schoolCode, 

    @Size(max = 255, message = "Tên trường không được vượt quá 255 ký tự")
    String schoolName, 

    @Size(max = 512, message = "Địa chỉ trường không được vượt quá 512 ký tự")
    String schoolAddress, 

    @Size(max = 100, message = "Tên miền của trường không được vượt quá 100 ký tự")
    String schoolDomain, 

    @NotNull(message = "Số học sinh trong trường không được để trống")
    @Min(value = 1, message = "Số lượng học sinh không được nhỏ hơn 1")
    Integer studentCount, 

    @NotBlank(message = "Email của quản trị viên nhà trường không được để trống")
    @Size(max = 255, message = "Email của quản trị viên nhà trường không được vượt quá 255 ký tự")
    String adminEmail, 

    @Size(max = 20, message = "Số điện thoại của quản trị viên nhà trường không được vượt quá 20 ký tự")
    String adminPhone, 

    @NotBlank(message = "Tên đầy đủ của quản trị viên nhà trường không được để trống")
    @Size(max = 255, message = "Tên đầy đủ của quản trị viên nhà trường không được vượt quá 255 ký tự")
    String adminFullName, 

    @NotBlank(message = "Ngày tháng năm sinh của quản trị viên nhà trường không được để trống")
    String adminDateOfBirth, 

    @Size(max = 255, message = "Địa chỉ của quản trị viên nhà trường không được vượt quá 255 ký tự")
    String adminAddress, 

    @Size(max = 4096, message = "Url ảnh đại diện của quản trị viên nhà trường không được vượt quá 4096 ký tự")
    String adminAvatarUrl
) {
    
}
