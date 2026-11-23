package com.sep.vox.application.port.input.query.recording;

import java.util.UUID;

public record GetAssessmentRecordsQuery(
    UUID assessmentSessionId, 
    String streamType
) {
    
}
