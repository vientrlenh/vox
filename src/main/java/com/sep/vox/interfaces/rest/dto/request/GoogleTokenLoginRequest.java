package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.GoogleTokenLoginCommand;
import com.sep.vox.domain.model.platform.Platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Đăng nhập Google từ ứng dụng native.
 *
 * <p>KHÔNG có {@code @Size} cho {@code idToken}, khác với mọi chuỗi khác trong gói này: đây là JWT
 * do Google phát, độ dài do họ quyết và có thể đổi bất cứ lúc nào (thêm claim là dài thêm). Một cái
 * trần đoán mò ở đây sẽ chặn đăng nhập thật vào một ngày Google thêm trường mới, mà không đổi lấy
 * điều gì -- token có bị cắt hay bơm phồng thì cũng trượt ngay ở phép kiểm chữ ký.
 */
public record GoogleTokenLoginRequest(
    @NotBlank(message = "Google token ID is required")
    String idToken,

    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    String deviceId, 

    @NotBlank(message = "Device name is required")
    @Size(max = 255, message = "Device name must not exceed 255 characters")
    String deviceName, 

    @NotNull(message = "Device platform is required")
    Platform platform
) {

    public static GoogleTokenLoginCommand toCommand(GoogleTokenLoginRequest request, String ipAddress, String userAgent) {
        return new GoogleTokenLoginCommand(
            request.idToken, 
            ipAddress, 
            userAgent, 
            request.deviceId, 
            request.deviceName, 
            request.platform
        );
    }
}
