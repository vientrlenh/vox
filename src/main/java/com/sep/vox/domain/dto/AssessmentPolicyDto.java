package com.sep.vox.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.PositiveLong;

public record AssessmentPolicyDto(
    UUID id,
    UUID schoolId,
    UUID gradeLevelId,
    UUID schoolGradeId,
    UUID schoolClassId,
    UUID learningLanguageId,
    UUID frameworkVersionId,
    UUID rubricVersionId,
    UUID targetFrameworkBandId,
    Long version,
    AssessmentPolicyStatus status,
    LocalDate effectiveFrom,
    Instant createdAt
) {

    public static AssessmentPolicyDto toDto(AssessmentPolicy assessmentPolicy) {
        return new AssessmentPolicyDto(
            assessmentPolicy.getId(),
            assessmentPolicy.getSchoolId(),
            assessmentPolicy.getGradeLevelId(),
            assessmentPolicy.getSchoolGradeId(),
            assessmentPolicy.getSchoolClassId(),
            assessmentPolicy.getLearningLanguageId(),
            assessmentPolicy.getFrameworkVersionId(),
            assessmentPolicy.getRubricVersionId(),
            assessmentPolicy.getTargetFrameworkBandId(),
            PositiveLong.valueOf(assessmentPolicy.getVersion()),
            assessmentPolicy.getStatus(),
            assessmentPolicy.getEffectiveFrom(),
            assessmentPolicy.getCreatedAt()
        );
    }

    public static List<AssessmentPolicyDto> toDtoList(List<AssessmentPolicy> policies) {
        return policies.stream()
                .map(AssessmentPolicyDto::toDto)
                .toList();
    }

    public static PageResult<AssessmentPolicyDto> toDtoPage(PageResult<AssessmentPolicy> policyPage) {
        return new PageResult<>(
                toDtoList(policyPage.content()),
                policyPage.page(),
                policyPage.size(),
                policyPage.totalElements(),
                policyPage.totalPages()
        );
    }
}
