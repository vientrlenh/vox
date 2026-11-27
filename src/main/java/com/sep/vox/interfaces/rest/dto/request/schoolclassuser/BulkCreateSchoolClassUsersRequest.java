package com.sep.vox.interfaces.rest.dto.request.schoolclassuser;

import com.sep.vox.application.port.input.command.schoolclassuser.BulkCreateSchoolClassUsersCommand;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkCreateSchoolClassUsersRequest(
    @NotEmpty(message = "User list must not be blank")
    @Size(max = 200, message = "At most 200 users can be added at a time")
    List<UUID> userIds
) {

    public static BulkCreateSchoolClassUsersCommand toCommand(UUID schoolId, UUID classId,
            BulkCreateSchoolClassUsersRequest request) {
        return new BulkCreateSchoolClassUsersCommand(schoolId, classId, request.userIds());
    }
}
