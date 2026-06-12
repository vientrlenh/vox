package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.command.ChangeSystemRubricVersionStatusCommand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.interfaces.rest.dto.request.RubricStatusRequest;

import java.util.UUID;

public class ChangeRubricVersionStatusCommandMapper {

    // SYSTEM
    public static ChangeSystemRubricVersionStatusCommand fromSystemRequest(UUID versionId, RubricStatus requestStatus) {
        return new ChangeSystemRubricVersionStatusCommand(versionId, requestStatus);
    }

    //SCHOOL
    public static ChangeSchoolRubricVersionStatusCommand fromSchoolRequest(UUID schoolId, UUID versionId, RubricStatus status) {
        return new ChangeSchoolRubricVersionStatusCommand(schoolId, versionId, status);
    }
}
