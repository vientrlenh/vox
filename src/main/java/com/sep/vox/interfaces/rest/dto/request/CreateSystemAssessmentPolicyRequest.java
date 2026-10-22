package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSystemAssessmentPolicyRequest(
        @NotNull(message = "Framework version ID is required")
        UUID frameworkVersionId,

        @NotNull(message = "Rubric version ID is required")
        UUID rubricVersionId,

        @NotNull(message = "Learning language ID is required")
        UUID languageId,

        @NotNull(message = "Target framework ID is required")
        UUID targetFrameworkBandId,

        @DecimalMin(value = "0.0", message = "Passing score must not be lower than 0.0")
        BigDecimal passingScore,

        @NotBlank(message = "Effective date from is required")
        String effectiveFrom,

        String effectiveTo
) {

}