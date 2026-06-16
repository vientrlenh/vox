package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;

public final class DeleteSchoolUserCommandMapper {

    public static DeleteSchoolUserCommand fromRequest(UUID schoolId, UUID userId) {
        return new DeleteSchoolUserCommand(schoolId, userId);
    }
}
