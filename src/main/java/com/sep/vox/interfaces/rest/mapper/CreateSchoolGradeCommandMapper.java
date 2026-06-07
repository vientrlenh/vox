package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;

import java.util.UUID;

public class CreateSchoolGradeCommandMapper {

    public static CreateSchoolGradeCommand fromRequest(UUID schoolId, CreateSchoolGradeRequest request) {
        return new CreateSchoolGradeCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }
}