package com.sep.vox.interfaces.rest.dto.request.assessmentpaper;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import com.sep.vox.interfaces.rest.dto.request.assessment.ClassTestQuestionRequest;

public record ClassTestSectionRequest(
    @NotBlank(message = "Section title is required")
    String title,

    String instruction,

    BigDecimal weight,

    @Valid
    @NotEmpty(message = "A section must have at least one question")
    List<ClassTestQuestionRequest> questions
) {
}
