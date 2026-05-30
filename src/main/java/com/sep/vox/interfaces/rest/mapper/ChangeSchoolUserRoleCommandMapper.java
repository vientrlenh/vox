package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;

public final class ChangeSchoolUserRoleCommandMapper {

    public static ChangeSchoolUserRoleCommand fromRequest(UUID schoolId, UUID userId, ChangeSchoolUserRoleRequest request) {
        return new ChangeSchoolUserRoleCommand(
            schoolId,
            userId,
            request.roleCode()
        );
    }
}
