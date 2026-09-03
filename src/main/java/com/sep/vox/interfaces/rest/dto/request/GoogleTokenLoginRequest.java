package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Đăng nhập Google từ ứng dụng native.
 *
 * <p>KHÔNG có {@code @Size} cho {@code idToken}, khác với mọi chuỗi khác trong gói này: đây là JWT
 * do Google phát, độ dài do họ quyết và có thể đổi bất cứ lúc nào (thêm claim là dài thêm). Một cái
 * trần đoán mò ở đây sẽ chặn đăng nhập thật vào một ngày Google thêm trường mới, mà không đổi lấy
 * điều gì -- token có bị cắt hay bơm phồng thì cũng trượt ngay ở phép kiểm chữ ký.
 */
public record GoogleTokenLoginRequest(
    @NotBlank(message = "ID token của Google là bắt buộc")
    String idToken,

    @Valid
    @NotNull(message = "Thông tin của thiết bị là bắt buộc")
    ClientDeviceRequest device
) {
}
