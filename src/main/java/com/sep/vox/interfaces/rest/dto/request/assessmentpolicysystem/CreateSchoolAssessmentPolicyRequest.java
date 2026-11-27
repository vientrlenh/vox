package com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem;

import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateSchoolAssessmentPolicyRequest(
        @NotNull(message = "Framework version must not be blank")
        UUID frameworkVersionId,

        @NotNull(message = "Rubric version must not be blank")
        UUID rubricVersionId,

        @NotNull(message = "Language must not be blank")
        UUID languageId,

        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,

        @NotNull(message = "Target band must not be blank")
        UUID targetFrameworkBandId,

        @DecimalMin(value = "0.0", message = "Passing score must be greater than or equal to 0")
        BigDecimal passingScore,

        AssessmentPolicyStrictness strictness,

        @NotNull(message = "Effective start date must not be blank")
        Instant effectiveFrom,

        Instant effectiveTo
) {

    // FOR SCHOOL ADMIN
    public static CreateAssessmentPolicyCommand toCommand(UUID schoolId, CreateSchoolAssessmentPolicyRequest request) {
        return new CreateAssessmentPolicyCommand(
                schoolId,
                request.frameworkVersionId(),
                request.rubricVersionId(),
                request.languageId(),
                request.gradeLevelId(),
                request.schoolGradeId(),
                request.schoolClassId(),
                request.targetFrameworkBandId(),
                request.passingScore(),
                request.strictness(),
                request.effectiveFrom(),
                request.effectiveTo()
        );
    }

    // FOR SCHOOL ADMIN: allows sending several Assessment Policies in one request
    public static List<CreateAssessmentPolicyCommand> toCommands(UUID schoolId, List<CreateSchoolAssessmentPolicyRequest> requests) {
        return requests.stream().map(request -> toCommand(schoolId, request)).toList();
    }
}
