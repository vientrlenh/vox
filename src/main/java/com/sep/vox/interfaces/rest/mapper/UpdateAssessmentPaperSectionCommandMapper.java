package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentPaperSectionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentPaperSectionRequest;

public final class UpdateAssessmentPaperSectionCommandMapper {

    private UpdateAssessmentPaperSectionCommandMapper() {
    }

    public static UpdateAssessmentPaperSectionCommand fromRequest(UUID paperId, UUID sectionId, UpdateAssessmentPaperSectionRequest request) {
        return new UpdateAssessmentPaperSectionCommand(paperId, sectionId, request.title(), request.instruction());
    }
}
