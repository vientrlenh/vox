package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập là bắt buộc") String login,

        @NotBlank(message = "Mật khẩu là bắt buộc") String password) {

}
