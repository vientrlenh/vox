package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;

public final class ListSchoolUsersCommandMapper {

    public static ListSchoolUsersCommand fromRequest(UUID schoolId, int page, int size) {
        return new ListSchoolUsersCommand(schoolId, page, size);
    }
}
