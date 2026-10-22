package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.model.school.SchoolUserRole;

public record ViewSchoolUsersBySchoolQuery(
    UUID schoolId,
    AppPageRequest pageRequest,
    String search,
    SchoolUserRole role,
    String status,
    UUID excludeClassId
) {

    public ViewSchoolUsersBySchoolQuery(UUID schoolId, AppPageRequest pageRequest, String search, SchoolUserRole role, String status) {
        this(schoolId, pageRequest, search, role, status, null);
    }
}
