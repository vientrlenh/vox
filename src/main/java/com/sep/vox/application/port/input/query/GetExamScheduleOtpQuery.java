package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record GetExamScheduleOtpQuery(
    UUID examId, 
    UUID scheduleId
) {
    
}
