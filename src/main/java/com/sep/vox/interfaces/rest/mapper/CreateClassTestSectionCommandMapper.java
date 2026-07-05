package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateClassTestSectionCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateClassTestSectionRequest;

public final class CreateClassTestSectionCommandMapper {

    private CreateClassTestSectionCommandMapper() {
    }

    public static CreateClassTestSectionCommand fromRequest(UUID examId, CreateClassTestSectionRequest request) {
        return new CreateClassTestSectionCommand(examId, request.title(), request.questionIds());
    }
}
