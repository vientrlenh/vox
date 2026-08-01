package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.BulkCreateSchoolClassUsersCommand;
import com.sep.vox.interfaces.rest.dto.request.BulkCreateSchoolClassUsersRequest;

public final class BulkCreateSchoolClassUsersCommandMapper {

    private BulkCreateSchoolClassUsersCommandMapper() {
    }

    public static BulkCreateSchoolClassUsersCommand fromRequest(UUID schoolId, UUID classId,
            BulkCreateSchoolClassUsersRequest request) {
        return new BulkCreateSchoolClassUsersCommand(schoolId, classId, request.userIds());
    }
}
