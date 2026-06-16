package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;

import java.util.UUID;

public class CreateSchoolGradeCommandMapper {

    // Thêm UUID schoolGradeLevelId vào tham số của hàm
    public static CreateSchoolGradeCommand fromRequest(UUID schoolId, UUID schoolGradeLevelId, CreateSchoolGradeRequest request) {
        return new CreateSchoolGradeCommand(
                schoolId,
                schoolGradeLevelId, // Truyền thêm thằng này vào đây!
                request.code(),
                request.name(),
                request.description(),
                DateMapper.toLocalDate(request.startDate()),
                DateMapper.toLocalDate(request.endDate())
        );
    }
}