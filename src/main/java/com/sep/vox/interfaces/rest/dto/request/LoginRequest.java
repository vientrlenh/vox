package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập là bắt buộc") String login,

        @NotBlank(message = "Mật khẩu là bắt buộc") String password, 

        @Valid
        @NotNull(message = "Thông tin của thiết bị là bắt buộc")
        ClientDeviceRequest device
) {

}

