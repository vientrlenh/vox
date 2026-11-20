package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamPaperSectionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperSectionRequest;

public final class UpdateExamPaperSectionCommandMapper {

    private UpdateExamPaperSectionCommandMapper() {
    }

    public static UpdateExamPaperSectionCommand fromRequest(UUID paperId, UUID sectionId, UpdateExamPaperSectionRequest request) {
        return new UpdateExamPaperSectionCommand(paperId, sectionId, request.title(), request.instruction());
    }
}
