package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleIdTokenLoginRequest(
        @NotBlank(message = "ID token là bắt buộc") String idToken,

        @Valid
        @NotNull(message = "Thông tin của thiết bị là bắt buộc")
        ClientDeviceRequest device
) {

}
