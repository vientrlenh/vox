package com.sep.vox.application.response.input.examschedule;

public record GetExamScheduleOtpResponse(
    String otp, 
    Long expiresAt
) {
    
}
