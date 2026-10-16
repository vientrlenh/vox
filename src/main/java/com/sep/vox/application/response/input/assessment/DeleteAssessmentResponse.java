package com.sep.vox.application.response.input.assessment;

public record DeleteAssessmentResponse(
    boolean deleted,
    boolean cancelledInstead
) {
}
