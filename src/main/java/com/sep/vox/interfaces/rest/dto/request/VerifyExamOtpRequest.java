package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyExamOtpRequest(
    @NotBlank String otp
) {
}
