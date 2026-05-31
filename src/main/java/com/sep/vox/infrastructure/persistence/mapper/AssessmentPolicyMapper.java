package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyOwnerType;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.infrastructure.persistence.entity.AssessmentPolicyJpaEntity;

public final class AssessmentPolicyMapper {

    private AssessmentPolicyMapper() {}

    public static AssessmentPolicy toDomain(AssessmentPolicyJpaEntity jpa) {
        return new AssessmentPolicy(
            jpa.getId(),
            fromOwnerType(jpa.getOwnerType()),
            jpa.getSchoolId(),
            jpa.getRubricVersionId(),
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
            valueOf(policy.getOwnerType()),
            policy.getSchoolId(),
            policy.getRubricVersionId(),
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

    private static String valueOf(AssessmentPolicyOwnerType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(AssessmentPolicyStatus status) {
        return status == null ? null : status.name();
    }

    private static AssessmentPolicyOwnerType fromOwnerType(String value) {
        return value == null ? null : AssessmentPolicyOwnerType.valueOf(value);
    }

    private static AssessmentPolicyStatus fromStatus(String value) {
        return value == null ? null : AssessmentPolicyStatus.valueOf(value);
    }
}
