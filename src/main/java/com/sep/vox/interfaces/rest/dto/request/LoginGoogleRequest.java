package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginGoogleRequest(
        @NotBlank(message = "idToken không được để trống")
        String idToken,

        @NotNull(message = "Thông tin thiết bị (clientDevice) không được để trống")
        ClientDeviceRequest clientDevice
) {}