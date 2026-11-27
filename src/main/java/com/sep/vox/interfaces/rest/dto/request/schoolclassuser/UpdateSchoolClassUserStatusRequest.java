package com.sep.vox.interfaces.rest.dto.request.schoolclassuser;

import com.sep.vox.application.port.input.command.schoolclassuser.UpdateSchoolClassUserStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record UpdateSchoolClassUserStatusRequest(
    @NotNull(message = "Status is required") Boolean isActive
) {

    public static UpdateSchoolClassUserStatusCommand toCommand(
            UUID schoolId,
            UUID classId,
            UUID userId,
            UpdateSchoolClassUserStatusRequest request) {
        return new UpdateSchoolClassUserStatusCommand(schoolId, classId, userId, request.isActive());
    }
}
