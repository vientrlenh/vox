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
        @NotNull(message = "Phiên bản Khung tiêu chuẩn không được để trống")
        UUID frameworkVersionId,

        @NotNull(message = "Phiên bản Rubric không được để trống")
        UUID rubricVersionId,

        @NotNull(message = "Ngôn ngữ không được để trống")
        UUID languageId,

        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,

        @NotNull(message = "Band mục tiêu không được để trống")
        UUID targetFrameworkBandId,

        @DecimalMin(value = "0.0", message = "Điểm đạt phải lớn hơn hoặc bằng 0")
        BigDecimal passingScore,

        AssessmentPolicyStrictness strictness,

        @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
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
