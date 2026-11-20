package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateClassTestSectionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestSectionRequest;

public final class UpdateClassTestSectionCommandMapper {

    private UpdateClassTestSectionCommandMapper() {
    }

    public static UpdateClassTestSectionCommand fromRequest(UUID assessmentId, UUID sectionId, UpdateClassTestSectionRequest request) {
        return new UpdateClassTestSectionCommand(
            assessmentId,
            sectionId,
            request.title(),
            request.instruction(),
            request.weight(),
            ClassTestQuestionCommandMapper.fromRequests(request.questions())
        );
    }
}
