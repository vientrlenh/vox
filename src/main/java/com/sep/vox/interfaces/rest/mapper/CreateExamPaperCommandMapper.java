package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamPaperCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamPaperRequest;

public final class CreateExamPaperCommandMapper {

    private CreateExamPaperCommandMapper() {
    }

    public static CreateExamPaperCommand fromRequest(UUID examId, CreateExamPaperRequest request) {
        if (request == null) {
            return new CreateExamPaperCommand(examId, null, null, null);
        }
        return new CreateExamPaperCommand(
            examId,
            request.source(),
            request.copyFromPaperId(),
            request.sections() == null ? null : request.sections().stream()
                .map(section -> new ClassTestSectionCommand(
                    section.title(),
                    section.instruction(),
                    section.weight(),
                    ClassTestQuestionCommandMapper.fromRequests(section.questions())
                ))
                .toList()
        );
    }
}
