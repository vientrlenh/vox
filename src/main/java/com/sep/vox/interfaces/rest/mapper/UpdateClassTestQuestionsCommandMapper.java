package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.UpdateClassTestQuestionsCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestQuestionsRequest;

public final class UpdateClassTestQuestionsCommandMapper {

    private UpdateClassTestQuestionsCommandMapper() {
    }

    public static UpdateClassTestQuestionsCommand fromRequest(UUID assessmentId, UpdateClassTestQuestionsRequest request) {
        List<ClassTestSectionCommand> sections = request.sections().stream()
            .map(section -> new ClassTestSectionCommand(
                section.title(),
                section.instruction(),
                section.weight(),
                ClassTestQuestionCommandMapper.fromRequests(section.questions())
            ))
            .toList();
        return new UpdateClassTestQuestionsCommand(assessmentId, request.paperId(), sections);
    }
}
