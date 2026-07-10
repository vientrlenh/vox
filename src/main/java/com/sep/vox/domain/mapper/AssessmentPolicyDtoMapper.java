package com.sep.vox.domain.mapper;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;

import java.util.List;

public class AssessmentPolicyDtoMapper {

    public static AssessmentPolicyDto toDto(AssessmentPolicy policy) {
        if (policy == null) {
            return null;
        }
        return new AssessmentPolicyDto(
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
                policy.getStrictness() != null ? policy.getStrictness().name() : null,
                policy.getVersion(),
                policy.getStatus() != null ? policy.getStatus().name() : null,
                policy.getEffectiveFrom(),
                policy.getEffectiveTo(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    public static List<AssessmentPolicyDto> toDtoList(List<AssessmentPolicy> policies) {
        return policies.stream()
                .map(AssessmentPolicyDtoMapper::toDto)
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
