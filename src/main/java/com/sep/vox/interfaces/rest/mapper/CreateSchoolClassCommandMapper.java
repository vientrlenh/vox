package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;

public final class CreateSchoolClassCommandMapper {

    public static CreateSchoolClassCommand fromRequest(UUID schoolId, CreateSchoolClassRequest request) {
        return new CreateSchoolClassCommand(
            schoolId,
            request.languageId(),
            request.schoolGradeId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
