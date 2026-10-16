package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;

public record VerifyAssessmentOtpRequest(
    @NotBlank(message = "OTP không được để trống") String otp
) {
}
