package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.ClassTestQuestionCommand;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ClassTestQuestionRequest(
    @NotNull(message = "questionId là bắt buộc")
    UUID questionId,

    @NotNull(message = "weight là bắt buộc")
    BigDecimal weight
) {

    public static List<ClassTestQuestionCommand> toCommands(List<ClassTestQuestionRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
            .map(request -> new ClassTestQuestionCommand(request.questionId(), request.weight()))
            .toList();
    }
}
