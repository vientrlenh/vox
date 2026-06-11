package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.infrastructure.persistence.entity.AssessmentPolicyJpaEntity;

public final class AssessmentPolicyMapper {

    private AssessmentPolicyMapper() {}

    public static AssessmentPolicy toDomain(AssessmentPolicyJpaEntity jpa) {
        return new AssessmentPolicy(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSchoolGradeLevelId(),
            jpa.getSchoolGradeId(),
            jpa.getSchoolClassId(),
            jpa.getLanguageId(),
            jpa.getFrameworkVersionId(),
            jpa.getRubricVersionId(),
            jpa.getTargetFrameworkBandId(),
            jpa.getMinimumFrameworkBandId(),
            jpa.getPassingScore(),
            fromStrictness(jpa.getStrictness()),
            jpa.getVersion(),
            fromStatus(jpa.getStatus()),
            jpa.getEffectiveFrom(),
            jpa.getEffectiveTo(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentPolicyJpaEntity toJpa(AssessmentPolicy policy) {
        return new AssessmentPolicyJpaEntity(
            policy.getId(),
            policy.getSchoolId(),
            policy.getSchoolGradeLevelId(),
            policy.getSchoolGradeId(),
            policy.getSchoolClassId(),
            policy.getLanguageId(),
            policy.getFrameworkVersionId(),
            policy.getRubricVersionId(),
            policy.getTargetFrameworkBandId(),
            policy.getMinimumFrameworkBandId(),
            policy.getPassingScore(),
            valueOf(policy.getStrictness()),
            policy.getVersion(),
            valueOf(policy.getStatus()),
            policy.getEffectiveFrom(),
            policy.getEffectiveTo(),
            policy.getCreatedAt(),
            policy.getUpdatedAt(),
            policy.getCreatedBy(),
            policy.getUpdatedBy()
        );
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
