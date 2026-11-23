package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.UpdateClassTestSectionCommand;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;

public record UpdateClassTestSectionRequest(
    String title,
    String instruction,
    BigDecimal weight,
    List<ClassTestQuestionRequest> questions
) {

    public static UpdateClassTestSectionCommand toCommand(UUID assessmentId, UUID sectionId, UpdateClassTestSectionRequest request) {
        return new UpdateClassTestSectionCommand(
            assessmentId,
            sectionId,
            request.title(),
            request.instruction(),
            request.weight(),
            ClassTestQuestionRequest.toCommands(request.questions())
        );
    }
}
