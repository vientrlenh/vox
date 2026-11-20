package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;

import java.util.UUID;

public class CreateSchoolGradeCommandMapper {

    // Takes the grade level id as an extra parameter
    public static CreateSchoolGradeCommand fromRequest(UUID schoolId, UUID gradeLevelId, CreateSchoolGradeRequest request) {
        return new CreateSchoolGradeCommand(
                schoolId,
                gradeLevelId, // Pass the grade level id through
                request.code(),
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}