package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.infrastructure.persistence.entity.SchoolQuotaPolicyJpaEntity;

public final class SchoolQuotaPolicyMapper {

    private SchoolQuotaPolicyMapper() {}

    public static SchoolQuotaPolicy toDomain(SchoolQuotaPolicyJpaEntity jpa) {
        return new SchoolQuotaPolicy(
            jpa.getId(),
            jpa.getSchoolId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getDistributableRatio(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }

    public static SchoolQuotaPolicyJpaEntity toJpa(SchoolQuotaPolicy domain) {
        return new SchoolQuotaPolicyJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getQuotaType() == null ? null : domain.getQuotaType().name(),
            domain.getDistributableRatio(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
