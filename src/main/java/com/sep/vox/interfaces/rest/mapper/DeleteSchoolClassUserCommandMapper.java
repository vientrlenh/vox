package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.DeleteSchoolClassUserCommand;

public final class DeleteSchoolClassUserCommandMapper {

    private DeleteSchoolClassUserCommandMapper() {
    }

    public static DeleteSchoolClassUserCommand fromPath(UUID schoolId, UUID classId, UUID userId) {
        return new DeleteSchoolClassUserCommand(schoolId, classId, userId);
    }
}
