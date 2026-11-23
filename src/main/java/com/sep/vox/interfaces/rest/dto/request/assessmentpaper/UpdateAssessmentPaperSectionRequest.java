package com.sep.vox.interfaces.rest.dto.request.assessmentpaper;

import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperSectionCommand;
import java.util.UUID;

public record UpdateAssessmentPaperSectionRequest(
    String title,
    String instruction
) {

    public static UpdateAssessmentPaperSectionCommand toCommand(UUID paperId, UUID sectionId, UpdateAssessmentPaperSectionRequest request) {
        return new UpdateAssessmentPaperSectionCommand(paperId, sectionId, request.title(), request.instruction());
    }
}
