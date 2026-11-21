package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolJpaEntity;

public final class SchoolMapper {
    
    public static School toDomain(SchoolJpaEntity jpa) {
        return School.builder()
            .id(jpa.getId())
            .code(new SchoolCode(jpa.getCode()))
            .name(jpa.getName())
            .description(jpa.getDescription())
            .contactPhone(new Phone(jpa.getContactPhone()))
            .contactEmail(new Email(jpa.getContactEmail()))
            .domain(new SchoolDomain(jpa.getDomain()))
            .address(jpa.getAddress())
            .studentCount(new StudentCount(jpa.getStudentCount()))
            .active(jpa.isActive())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static SchoolJpaEntity toJpa(School school) {
        return SchoolJpaEntity.builder()
            .id(school.getId())
            .code(valueOf(school.getCode()))
            .name(school.getName())
            .description(school.getDescription())
            .contactPhone(valueOf(school.getContactPhone()))
            .contactEmail(valueOf(school.getContactEmail()))
            .domain(valueOf(school.getDomain()))
            .address(school.getAddress())
            .studentCount(valueOf(school.getStudentCount()))
            .isActive(school.isActive())
            .createdAt(school.getCreatedAt())
            .updatedAt(school.getUpdatedAt())
            .createdBy(school.getCreatedBy())
            .updatedBy(school.getUpdatedBy())
            .build();
    }

    private static String valueOf(SchoolCode schoolCode) {
        return schoolCode == null ? null : schoolCode.value();
    }

    private static String valueOf(Phone phone) {
        return phone == null ? null : phone.value();
    }

    private static String valueOf(Email email) {
        return email == null ? null : email.value();
    }

    private static String valueOf(SchoolDomain schoolDomain) {
        return schoolDomain == null ? null : schoolDomain.value();
    }

    private static int valueOf(StudentCount studentCount) {
        return studentCount == null ? 0 : studentCount.value();
    }
}
