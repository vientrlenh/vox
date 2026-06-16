package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;

public final class ViewSchoolUserCommandMapper {

    public static ViewSchoolUserCommand fromRequest(UUID schoolId, UUID userId) {
        return new ViewSchoolUserCommand(schoolId, userId);
    }
}
