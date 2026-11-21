package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolBalanceJpaEntity;

public final class SchoolBalanceMapper {

    private SchoolBalanceMapper() {}

    public static SchoolBalance toDomain(SchoolBalanceJpaEntity jpa) {
        return SchoolBalance.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .balanceVnd(jpa.getBalanceVnd())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .version(jpa.getVersion())
            .build();
    }

    public static SchoolBalanceJpaEntity toJpa(SchoolBalance domain) {
        return SchoolBalanceJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .balanceVnd(domain.getBalanceVnd())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .version(domain.getVersion())
            .build();
    }
}
