package com.sep.vox.application.mapper.schoolclassuser;

import java.time.OffsetDateTime;

import com.sep.vox.application.response.input.schoolclassuser.SchoolClassUserResponse;
import com.sep.vox.domain.model.school.SchoolClassUser;

public final class SchoolClassUserResponseMapper {

    private SchoolClassUserResponseMapper() {
    }

    public static SchoolClassUserResponse toResponse(SchoolClassUser schoolClassUser) {
        return new SchoolClassUserResponse(
            schoolClassUser.getId(),
            schoolClassUser.getUserId(),
            schoolClassUser.getSchoolClassId(),
            schoolClassUser.isActive(),
            valueOf(schoolClassUser.getJoinedAt()),
            valueOf(schoolClassUser.getLeftAt()),
            schoolClassUser.getAssignedBy(),
            null
        );
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
