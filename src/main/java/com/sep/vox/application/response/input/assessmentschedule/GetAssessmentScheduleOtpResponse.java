package com.sep.vox.application.response.input.assessmentschedule;

public record GetAssessmentScheduleOtpResponse(
    String otp, 
    Long expiresAt
) {
    
}
