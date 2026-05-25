package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;

public final class SchoolDtoMapper {
    
    public static SchoolDto toSchoolDto(School school) {
        return new SchoolDto(
            school.getId(), 
            valueOf(school.getCode()), 
            school.getName(), 
            school.getDescription(), 
            valueOf(school.getContactPhone()), 
            valueOf(school.getContactEmail()), 
            valueOf(school.getDomain()), 
            school.getAddress(), 
            valueOf(school.getStudentCount()), 
            school.isActive()
        );
    }

    public static List<SchoolDto> toSchoolDtoList(List<School> schools) {
        return schools.stream()
            .map(SchoolDtoMapper::toSchoolDto)
            .toList();
    }

    public static PageResult<SchoolDto> toSchoolDtoPage(PageResult<School> schoolPage) {
        return new PageResult<>(
            toSchoolDtoList(schoolPage.content()),
            schoolPage.page(),
            schoolPage.size(),
            schoolPage.totalElements(),
            schoolPage.totalPages()
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
