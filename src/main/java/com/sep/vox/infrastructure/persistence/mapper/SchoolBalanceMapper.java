package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.infrastructure.persistence.entity.SchoolBalanceJpaEntity;

public final class SchoolBalanceMapper {

    private SchoolBalanceMapper() {}

    public static SchoolBalance toDomain(SchoolBalanceJpaEntity jpa) {
        return new SchoolBalance(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getBalanceVnd(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getVersion()
        );
    }

    public static SchoolBalanceJpaEntity toJpa(SchoolBalance domain) {
        return new SchoolBalanceJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getBalanceVnd(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getVersion()
        );
    }
}
