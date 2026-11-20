package com.sep.vox.interfaces.rest.dto.request;

public record UpdateAssessmentPaperSectionRequest(
    String title,
    String instruction
) {
}
