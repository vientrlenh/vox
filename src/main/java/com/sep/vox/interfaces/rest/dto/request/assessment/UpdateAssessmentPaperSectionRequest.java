package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentPaperSectionCommand;

public record UpdateAssessmentPaperSectionRequest(
    String title,
    String instruction
) {

    public static UpdateAssessmentPaperSectionCommand toCommand(UUID paperId, UUID sectionId, UpdateAssessmentPaperSectionRequest request) {
        return new UpdateAssessmentPaperSectionCommand(paperId, sectionId, request.title(), request.instruction());
    }
}
