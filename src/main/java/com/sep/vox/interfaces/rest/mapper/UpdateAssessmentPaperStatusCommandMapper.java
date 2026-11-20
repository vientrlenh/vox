package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentPaperStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentPaperStatusRequest;

public final class UpdateAssessmentPaperStatusCommandMapper {

    private UpdateAssessmentPaperStatusCommandMapper() {
    }

    public static UpdateAssessmentPaperStatusCommand fromRequest(UUID paperId, UpdateAssessmentPaperStatusRequest request) {
        return new UpdateAssessmentPaperStatusCommand(paperId, request.action(), request.note());
    }
}
