package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record GetAssessmentScheduleOtpQuery(
    UUID assessmentId, 
    UUID scheduleId
) {
    
}
