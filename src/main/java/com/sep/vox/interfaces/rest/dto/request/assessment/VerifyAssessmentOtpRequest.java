package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;

public record VerifyAssessmentOtpRequest(
    @NotBlank(message = "OTP must not be blank") String otp
) {
}
