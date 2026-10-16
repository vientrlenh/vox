package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintSectionCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentBlueprintSectionRequest(
    @NotNull(message = "Thứ tự section là bắt buộc")
    Integer order,

    @NotBlank(message = "Tiêu đề section là bắt buộc")
    String title,

    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight
) {

    public static UpdateAssessmentBlueprintSectionCommand toCommand(UUID sectionId, UpdateAssessmentBlueprintSectionRequest request) {
        return new UpdateAssessmentBlueprintSectionCommand(
            sectionId,
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight()
        );
    }
}
