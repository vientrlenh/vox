package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.CreateClassTestSectionCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateClassTestSectionRequest(
    /** Paper code that receives the new section. Empty is valid only when the test has exactly one paper code. */
    UUID paperId,

    @NotBlank(message = "Section title is required")
    String title,

    String instruction,

    BigDecimal weight,

    @Valid
    @NotEmpty(message = "A section must have at least one question")
    List<ClassTestQuestionRequest> questions
) {

    public static CreateClassTestSectionCommand toCommand(UUID assessmentId, CreateClassTestSectionRequest request) {
        return new CreateClassTestSectionCommand(
            assessmentId,
            request.paperId(),
            request.title(),
            request.instruction(),
            request.weight(),
            ClassTestQuestionRequest.toCommands(request.questions())
        );
    }
}
