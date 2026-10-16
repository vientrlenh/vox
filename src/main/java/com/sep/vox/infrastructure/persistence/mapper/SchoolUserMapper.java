package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolUserJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class SchoolUserMapper {

    private SchoolUserMapper() {}
    
    public static SchoolUser toDomain(SchoolUserJpaEntity jpa) {
        try {
            return SchoolUser.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .schoolId(jpa.getSchoolId())
                .memberCode(Code.from(jpa.getMemberCode()))
                .role(SchoolUserRole.from(jpa.getRole()))
                .startDate(jpa.getStartDate())
                .endDate(jpa.getEndDate())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school user entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static SchoolUserJpaEntity toJpa(SchoolUser schoolUser) {
        try {
            return SchoolUserJpaEntity.builder()
                .id(schoolUser.getId())
                .userId(schoolUser.getUserId())
                .schoolId(schoolUser.getSchoolId())
                .memberCode(Code.valueOf(schoolUser.getMemberCode()))
                .role(SchoolUserRole.value(schoolUser.getRole()))
                .startDate(schoolUser.getStartDate())
                .endDate(schoolUser.getEndDate())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school user model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
