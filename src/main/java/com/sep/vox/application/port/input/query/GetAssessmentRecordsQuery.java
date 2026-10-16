package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record GetAssessmentRecordsQuery(
    UUID assessmentSessionId, 
    String streamType
) {
    
}
