package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.infrastructure.persistence.entity.assessmentpolicy.AssessmentPolicyJpaEntity;

public final class AssessmentPolicyMapper {

    private AssessmentPolicyMapper() {}

    public static AssessmentPolicy toDomain(AssessmentPolicyJpaEntity jpa) {
        return AssessmentPolicy.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .gradeLevelId(jpa.getGradeLevelId())
            .schoolGradeId(jpa.getSchoolGradeId())
            .schoolClassId(jpa.getSchoolClassId())
            .languageId(jpa.getLanguageId())
            .frameworkVersionId(jpa.getFrameworkVersionId())
            .rubricVersionId(jpa.getRubricVersionId())
            .targetFrameworkBandId(jpa.getTargetFrameworkBandId())
            .passingScore(jpa.getPassingScore())
            .strictness(fromStrictness(jpa.getStrictness()))
            .version(jpa.getVersion())
            .status(fromStatus(jpa.getStatus()))
            .effectiveFrom(jpa.getEffectiveFrom())
            .effectiveTo(jpa.getEffectiveTo())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static AssessmentPolicyJpaEntity toJpa(AssessmentPolicy policy) {
        return AssessmentPolicyJpaEntity.builder()
            .id(policy.getId())
            .schoolId(policy.getSchoolId())
            .gradeLevelId(policy.getGradeLevelId())
            .schoolGradeId(policy.getSchoolGradeId())
            .schoolClassId(policy.getSchoolClassId())
            .languageId(policy.getLanguageId())
            .frameworkVersionId(policy.getFrameworkVersionId())
            .rubricVersionId(policy.getRubricVersionId())
            .targetFrameworkBandId(policy.getTargetFrameworkBandId())
            .passingScore(policy.getPassingScore())
            .strictness(valueOf(policy.getStrictness()))
            .version(policy.getVersion())
            .status(valueOf(policy.getStatus()))
            .effectiveFrom(policy.getEffectiveFrom())
            .effectiveTo(policy.getEffectiveTo())
            .createdAt(policy.getCreatedAt())
            .updatedAt(policy.getUpdatedAt())
            .createdBy(policy.getCreatedBy())
            .updatedBy(policy.getUpdatedBy())
            .build();
    }

    private static String valueOf(AssessmentPolicyStrictness strictness) {
        return strictness == null ? null : strictness.name();
    }

    private static String valueOf(AssessmentPolicyStatus status) {
        return status == null ? null : status.name();
    }

    private static AssessmentPolicyStrictness fromStrictness(String value) {
        return value == null ? null : AssessmentPolicyStrictness.valueOf(value);
    }

    private static AssessmentPolicyStatus fromStatus(String value) {
        return value == null ? null : AssessmentPolicyStatus.valueOf(value);
    }
}
