package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.user.UserStatus;

public record ViewSchoolUsersBySchoolQuery(
    UUID schoolId,
    int page,
    int size,
    String search,
    UUID roleId,
    String roleCode,
    UserStatus status,
    UUID excludeClassId
) {

    public ViewSchoolUsersBySchoolQuery(UUID schoolId, int page, int size, String search, UUID roleId, UserStatus status) {
        this(schoolId, page, size, search, roleId, null, status, null);
    }
}
