package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolDirectoryRequest(
    @NotBlank(message = "Mã danh mục trường không được để trống")
    @Size(max = 100, message = "Mã danh mục trường không được vượt quá 100 ký tự")
    String code, 

    @NotBlank(message = "Tên danh mục trường không được để trống")
    @Size(max = 255, message = "Tên danh mục trường không được vượt quá 255 ký tự")
    String name, 

    @NotBlank(message = "Mã tỉnh/thành của danh mục trường không được để trống")
    @Size(max = 100, message = "Mã tỉnh/thành của danh mục trường không được vượt quá 100 ký tự")
    String provinceCode,

    @NotBlank(message = "Tên tỉnh/thành của danh mục trường không được để trống")
    @Size(max = 255, message = "Tên tỉnh/thành của danh mục trường không được vượt quá 255 ký tự")
    String provinceName, 

    @NotBlank(message = "Tên vùng của danh mục trường không được để trống")
    @Size(max = 255, message = "Tên vùng của danh mục trường không được vượt quá 255 ký tự")
    String districtName, 

    @Size(max = 100, message = "Tên miền của trường không được vượt quá 100 ký tự")
    String domain, 

    @Size(max = 512, message = "Địa chỉ của danh mục trường không được vượt quá 512 ký tự")
    String address
) {
    
}
