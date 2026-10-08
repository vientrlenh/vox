package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.SchoolDomain;

public record SchoolDto(
    UUID id,
    String code, 
    String name,
    String description,
    String domain,
    UUID wardId, 
    UUID cityId, 
    UUID provinceId, 
    String streetAddress,
    int studentCount,
    SchoolStatus status, 
    Instant createdAt, 
    Instant updatedAt, 
    UUID registeredBy
) {
    
    public static SchoolDto toDto(School school) {
        return new SchoolDto(
            school.getId(), 
            Code.valueOf(school.getCode()), 
            Name.valueOf(school.getName()), 
            school.getDescription(), 
            SchoolDomain.valueOf(school.getDomain()), 
            school.getWardId(), 
            school.getCityId(), 
            school.getProvinceId(), 
            school.getStreetAddress(), 
            PositiveInteger.valueOf(school.getStudentCount()), 
            school.getStatus(),
            school.getCreatedAt(), 
            school.getUpdatedAt(), 
            school.getRegisteredBy()
        );
    }

    public static List<SchoolDto> toDtoList(List<School> schools) {
        return schools.stream()
            .map(SchoolDto::toDto)
            .toList();
    }

    public static PageResult<SchoolDto> toDtoPage(PageResult<School> schoolPage) {
        List<SchoolDto> content = toDtoList(schoolPage.content());
        return PageResult.pageResult(
            content, 
            schoolPage.page(), 
            schoolPage.size(), 
            schoolPage.totalElements(), 
            schoolPage.totalPages()
        );
    }
}
