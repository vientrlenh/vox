package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.command.ChangeSystemRubricVersionStatusCommand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.interfaces.rest.dto.request.RubricStatusRequest;

import java.util.UUID;

public class ChangeRubricVersionStatusCommandMapper {

    // SYSTEM
    public static ChangeSystemRubricVersionStatusCommand fromSystemRequest(UUID versionId, RubricStatusRequest requestStatus) {
        // Dịch từ Enum REST sang Enum Domain
        RubricStatus domainStatus = RubricStatus.valueOf(requestStatus.name());
        return new ChangeSystemRubricVersionStatusCommand(versionId, domainStatus);
    }

    //SCHOOL
    public static ChangeSchoolRubricVersionStatusCommand fromSchoolRequest(UUID schoolId, UUID versionId, RubricStatusRequest status) {
        RubricStatus domainStatus = RubricStatus.valueOf(status.name());
        return new ChangeSchoolRubricVersionStatusCommand(schoolId, versionId, domainStatus);
    }
}
