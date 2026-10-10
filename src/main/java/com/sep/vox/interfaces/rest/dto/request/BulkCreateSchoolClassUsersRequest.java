package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.BulkCreateSchoolClassUsersCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkCreateSchoolClassUsersRequest(
    @NotEmpty(message = "School user list is required")
    @Size(max = 200, message = "Only 200 users are allowed to be assigned at the same time")
    List<UUID> userIds
) {

    public static BulkCreateSchoolClassUsersCommand toCommand(UUID schoolId, UUID schoolClassId, BulkCreateSchoolClassUsersRequest request) {
        return new BulkCreateSchoolClassUsersCommand(schoolId, schoolClassId, request.userIds);
    }
}
