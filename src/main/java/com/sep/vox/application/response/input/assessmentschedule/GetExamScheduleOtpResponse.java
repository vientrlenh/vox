package com.sep.vox.application.response.input.assessmentschedule;

public record GetExamScheduleOtpResponse(
    String otp, 
    Long expiresAt
) {
    
}
