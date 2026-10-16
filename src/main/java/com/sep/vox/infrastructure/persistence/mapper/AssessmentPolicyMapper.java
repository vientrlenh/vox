package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.AssessmentPolicyJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AssessmentPolicyMapper {

    private AssessmentPolicyMapper() {}

    public static AssessmentPolicy toDomain(AssessmentPolicyJpaEntity jpa) {
        try {
            return AssessmentPolicy.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .gradeLevelId(jpa.getGradeLevelId())
                .schoolGradeId(jpa.getSchoolGradeId())
                .schoolClassId(jpa.getSchoolClassId())
                .learningLanguageId(jpa.getLearningLanguageId())
                .frameworkVersionId(jpa.getFrameworkVersionId())
                .rubricVersionId(jpa.getRubricVersionId())
                .targetFrameworkBandId(jpa.getTargetFrameworkBandId())
                .passingScore(PositiveDecimal.from(jpa.getPassingScore()))
                .strictness(AssessmentPolicyStrictness.from(jpa.getStrictness()))
                .version(PositiveInteger.from(jpa.getVersion()))
                .status(AssessmentPolicyStatus.from(jpa.getStatus()))
                .effectiveFrom(jpa.getEffectiveFrom())
                .effectiveTo(jpa.getEffectiveTo())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping assessment policy entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static AssessmentPolicyJpaEntity toJpa(AssessmentPolicy policy) {
        try {
            return AssessmentPolicyJpaEntity.builder()
                .id(policy.getId())
                .schoolId(policy.getSchoolId())
                .gradeLevelId(policy.getGradeLevelId())
                .schoolGradeId(policy.getSchoolGradeId())
                .schoolClassId(policy.getSchoolClassId())
                .learningLanguageId(policy.getLearningLanguageId())
                .frameworkVersionId(policy.getFrameworkVersionId())
                .rubricVersionId(policy.getRubricVersionId())
                .targetFrameworkBandId(policy.getTargetFrameworkBandId())
                .passingScore(PositiveDecimal.valueOf(policy.getPassingScore()))
                .strictness(AssessmentPolicyStrictness.value(policy.getStrictness()))
                .version(PositiveInteger.valueOf(policy.getVersion()))
                .status(AssessmentPolicyStatus.value(policy.getStatus()))
                .effectiveFrom(policy.getEffectiveFrom())
                .effectiveTo(policy.getEffectiveTo())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping assessment policy model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
