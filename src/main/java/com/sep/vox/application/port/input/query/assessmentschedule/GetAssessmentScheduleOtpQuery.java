package com.sep.vox.application.port.input.query.assessmentschedule;

import java.util.UUID;

public record GetAssessmentScheduleOtpQuery(
    UUID assessmentId, 
    UUID scheduleId
) {
    
}
