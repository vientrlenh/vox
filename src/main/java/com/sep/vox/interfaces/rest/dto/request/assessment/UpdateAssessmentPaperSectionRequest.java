package com.sep.vox.interfaces.rest.dto.request.assessment;

public record UpdateAssessmentPaperSectionRequest(
    String title,
    String instruction
) {
}
