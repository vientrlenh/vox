package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.SchoolDirectoryDto;
import com.sep.vox.domain.model.school.SchoolDirectory;

public final class SchoolDirectoryDtoMapper {
    
    public static SchoolDirectoryDto toSchoolDirectoryDto(SchoolDirectory dir) {
        return new SchoolDirectoryDto(
            dir.getId(), 
            dir.getCode(), 
            dir.getName(), 
            dir.getProvinceCode(), 
            dir.getProvinceName(), 
            dir.getDistrictName(), 
            dir.getDomain(), 
            dir.getAddress(), 
            dir.getOrigin().name(), 
            dir.isVerified(), 
            dir.getCreatedAt().toString(), 
            dir.getUpdatedAt().toString()
        );
    }

    public static List<SchoolDirectoryDto> toSchoolDirectoryDtoList(List<SchoolDirectory> dirs) {
        return dirs.stream()
            .map(SchoolDirectoryDtoMapper::toSchoolDirectoryDto)
            .toList();
    }
}
