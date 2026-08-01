package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolUsersBySchoolQuery(
    UUID schoolId,
    int page,
    int size,
    String search,
    UUID roleId,
    String roleCode,
    String status,
    UUID excludeClassId
) {

    public ViewSchoolUsersBySchoolQuery(UUID schoolId, int page, int size, String search, UUID roleId, String status) {
        this(schoolId, page, size, search, roleId, null, status, null);
    }
}
