package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.GoogleTokenLoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Google login from a native app.
 *
 * <p>There is NO {@code @Size} on {@code idToken}, unlike every other string in this package: it is a JWT
 * issued by Google, whose length Google decides and can change at any time (adding a claim makes it longer). A
 * guessed ceiling here would block real logins the day Google adds a new field, and gain nothing in return
 * -- a truncated or inflated token fails at the signature check anyway.
 */
public record GoogleTokenLoginRequest(
    @NotBlank(message = "ID token của Google là bắt buộc")
    String idToken,

    @Valid
    @NotNull(message = "Thông tin của thiết bị là bắt buộc")
    ClientDeviceRequest device
) {

    public static GoogleTokenLoginCommand toCommand(GoogleTokenLoginRequest request, String ipAddress,
            String userAgent) {
        return new GoogleTokenLoginCommand(
            request.idToken(),
            ipAddress,
            userAgent,
            ClientDeviceRequest.toCommand(request.device())
        );
    }
}
