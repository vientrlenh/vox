package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import com.sep.vox.application.port.input.command.assessment.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.assessment.UpdateClassTestQuestionsCommand;
import com.sep.vox.interfaces.rest.dto.request.assessmentpaper.ClassTestSectionRequest;

public record UpdateClassTestQuestionsRequest(
    /** Paper code whose whole content is replaced. Empty is valid only when the test has exactly one paper code. */
    UUID paperId,

    @Valid
    @NotEmpty(message = "Section list must not be blank")
    List<ClassTestSectionRequest> sections
) {

    public static UpdateClassTestQuestionsCommand toCommand(UUID assessmentId, UpdateClassTestQuestionsRequest request) {
        List<ClassTestSectionCommand> sections = request.sections().stream()
            .map(section -> new ClassTestSectionCommand(
                section.title(),
                section.instruction(),
                section.weight(),
                ClassTestQuestionRequest.toCommands(section.questions())
            ))
            .toList();
        return new UpdateClassTestQuestionsCommand(assessmentId, request.paperId(), sections);
    }
}
