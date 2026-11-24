package com.sep.vox.application.port.input.command.stream;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

public record IssueStudentStreamTokenCommand( 
    UUID assessmentSessionId,
    AssessmentRequiredStreamType streamType
) {


}
