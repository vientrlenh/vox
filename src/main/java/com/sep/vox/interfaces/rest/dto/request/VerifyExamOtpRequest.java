package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyExamOtpRequest(
    @NotBlank(message = "OTP không được để trống") String otp
) {
}
