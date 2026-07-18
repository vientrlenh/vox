package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterPushTokenRequest(
    @NotBlank(message = "deviceId không được để trống")
    String deviceId,

    @NotBlank(message = "pushToken không được để trống")
    String pushToken
) {
}
