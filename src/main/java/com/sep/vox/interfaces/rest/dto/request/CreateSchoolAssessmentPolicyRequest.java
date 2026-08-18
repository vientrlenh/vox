package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSchoolAssessmentPolicyRequest(
        @NotNull(message = "Phiên bản Khung tiêu chuẩn không được để trống")
        UUID frameworkVersionId,

        @NotNull(message = "Phiên bản Rubric không được để trống")
        UUID rubricVersionId,

        @NotNull(message = "Ngôn ngữ không được để trống")
        UUID languageId,

        UUID schoolGradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,

        @NotNull(message = "Band mục tiêu không được để trống")
        UUID targetFrameworkBandId,

        @DecimalMin(value = "0.0", message = "Điểm đạt phải lớn hơn hoặc bằng 0")
        BigDecimal passingScore,

        AssessmentPolicyStrictness strictness,

        @NotBlank(message = "Ngày bắt đầu hiệu lực không được để trống")
        String effectiveFrom,

        String effectiveTo
) {
}