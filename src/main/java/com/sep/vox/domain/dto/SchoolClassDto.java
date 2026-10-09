package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.language.LearningLanguage;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;

public record SchoolClassDto(
        UUID id,
        UUID schoolId,
        LearningLanguage language,
        UUID schoolGradeId,
        String code,
        String name,
        String description,
        SchoolClassStatus status,
        Instant createdAt,
        Instant updatedAt
) {

        public static SchoolClassDto toDto(SchoolClass sClass) {
                return new SchoolClassDto(
                        sClass.getId(), 
                        sClass.getSchoolId(), 
                        sClass.getLanguage(), 
                        sClass.getSchoolGradeId(), 
                        Code.valueOf(sClass.getCode()), 
                        Name.valueOf(sClass.getName()), 
                        sClass.getDescription(), 
                        sClass.getStatus(), 
                        sClass.getCreatedAt(), 
                        sClass.getUpdatedAt()
                );
        } 
}
