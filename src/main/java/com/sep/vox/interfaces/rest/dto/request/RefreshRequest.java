package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "ID của thiết bị không được để trống")
    String deviceId,

    /// Chỉ dùng cho app di động -- Dio không tự quản cookie như trình duyệt nên
    /// không có gì gửi lại `refresh_token` qua cookie. Web vẫn đi qua cookie như
    /// cũ (field này để trống); controller ưu tiên cookie nếu có, rơi xuống field
    /// này khi không có cookie.
    String refreshToken
) {
}
