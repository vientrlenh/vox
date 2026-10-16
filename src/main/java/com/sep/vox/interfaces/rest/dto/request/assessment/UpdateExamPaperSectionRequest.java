package com.sep.vox.interfaces.rest.dto.request.assessment;

public record UpdateExamPaperSectionRequest(
    String title,
    String instruction
) {
}
