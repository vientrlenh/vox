package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;
import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;

public final class SchoolMapper {
    
    public static School toDomain(SchoolJpaEntity jpa) {
        return new School(
            jpa.getId(),
            new SchoolCode(jpa.getCode()),
            jpa.getName(),
            jpa.getDescription(),
            new Phone(jpa.getContactPhone()),
            new Email(jpa.getContactEmail()),
            new SchoolDomain(jpa.getDomain()),
            jpa.getAddress(),
            new StudentCount(jpa.getStudentCount()),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static SchoolJpaEntity toJpa(School school) {
        return new SchoolJpaEntity(
            school.getId(), 
            valueOf(school.getCode()), 
            school.getName(), 
            school.getDescription(), 
            valueOf(school.getContactPhone()), 
            valueOf(school.getContactEmail()), 
            valueOf(school.getDomain()), 
            school.getAddress(),
            valueOf(school.getStudentCount()), 
            school.isActive(), 
            school.getCreatedAt(), 
            school.getUpdatedAt(), 
            school.getCreatedBy(), 
            school.getUpdatedBy()
        );
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
