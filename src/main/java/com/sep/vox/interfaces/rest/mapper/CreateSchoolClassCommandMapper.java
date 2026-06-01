package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;

public final class CreateSchoolClassCommandMapper {

    public static CreateSchoolClassCommand fromRequest(CreateSchoolClassRequest request) {
        return new CreateSchoolClassCommand(
            request.languageId(),
            request.schoolGradeId(),
            request.code(),
            request.name(),
            request.description(),
            request.targetSchoolLevelVersionId()
        );
    }
}
