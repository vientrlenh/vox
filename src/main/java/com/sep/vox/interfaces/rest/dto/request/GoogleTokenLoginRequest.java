package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Đăng nhập Google từ app di động: idToken lấy native qua Google Sign-In SDK
/// (audience = client OAuth2 "google" đã đăng ký -- cùng client-id với luồng
/// redirect trình duyệt `/oauth2/google/start`), khác hẳn với luồng đó vì không
/// đi qua HttpSession/redirect nào cả.
public record GoogleTokenLoginRequest(
        @NotBlank(message = "idToken là bắt buộc") String idToken,

        @Valid
        @NotNull(message = "Thông tin của thiết bị là bắt buộc")
        ClientDeviceRequest device
) {

}
