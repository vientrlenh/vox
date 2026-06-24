package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterBySelfDeclaredRequest(
    @NotBlank(message = "Tên trường không được để trống")
    @Size(max = 255, message = "Tên trường không được vượt quá 255 ký tự")
    String schoolName, 

    @Size(max = 100, message = "Tên miền của trường không được vượt quá 100 ký tự")
    String schoolDomain, 

    @NotBlank(message = "Phường của trường không được để trống")
    @Size(max = 255, message = "Phường của trường không được vượt quá 255 ký tự")
    String schoolDistrict, 

    @NotBlank(message = "Tỉnh/thành phố của trường không được để trống")
    @Size(max = 255, message = "Tỉnh/thành phố của trường không được vượt quá 255 ký tự")
    String schoolProvince, 

    @NotBlank(message = "Địa chỉ trường không được để trống")
    @Size(max = 512, message = "Địa chỉ của trường không được vượt quá 512 ký tự")
    String schoolAddress, 

    @NotBlank(message = "Tên liên hệ không được để trống")
    @Size(max = 255, message = "Tên liên hệ không được vượt quá 255 ký tự")
    String contactFullName, 

    @NotBlank(message = "Mã định danh không được để trống")
    @Size(max = 20, message = "Mã định danh không được vượt quá 20 ký tự")    
    String identityNumber, 

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    String contactPhone,
    
    @NotBlank(message = "Email liên hệ không được để trống")
    @Size(max = 255, message = "Email liên hệ không được vượt quá 255 ký tự")
    @Email(message = "Email không hợp lệ")
    String contactEmail, 

    @NotBlank(message = "Ngày sinh không được để trống")
    String dateOfBirth, 

    @NotBlank(message = "Địa chỉ liên hệ không được để trống")
    @Size(max = 512, message = "Địa chỉ liên hệ không được vượt quá 512 ký tự")
    String contactAddress,
    
    @NotBlank(message = "Mã bưu chính không được để trống")
    @Size(max = 10, message = "Mã bưu chính không được vượt quá 10 ký tự")
    String postalCode, 

    @NotBlank(message = "Chức vụ không được để trống")
    @Size(max = 50, message = "Chức vụ không được vượt quá 50 ký tự")
    String position, 

    @NotNull(message = "Số học sinh không được để trống")
    @Min(value = 1, message = "Số học sinh không được nhỏ hơn 1")   
    @Max(value = Integer.MAX_VALUE, message = "Số học sinh không được vượt quá " + Integer.MAX_VALUE)
    Integer studentCount, 

    @NotNull(message = "Đường dẫn của tài liệu không được để trống")
    List<String> documentUrls
) {
    
}
