package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolQuotaPolicyJpaEntity;

public final class SchoolQuotaPolicyMapper {

    private SchoolQuotaPolicyMapper() {}

    public static SchoolQuotaPolicy toDomain(SchoolQuotaPolicyJpaEntity jpa) {
        return SchoolQuotaPolicy.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .quotaType(QuotaType.valueOf(jpa.getQuotaType()))
            .distributableRatio(jpa.getDistributableRatio())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .build();
    }

    public static SchoolQuotaPolicyJpaEntity toJpa(SchoolQuotaPolicy domain) {
        return SchoolQuotaPolicyJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .quotaType(domain.getQuotaType() == null ? null : domain.getQuotaType().name())
            .distributableRatio(domain.getDistributableRatio())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }
}
