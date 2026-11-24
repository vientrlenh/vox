package com.sep.vox.application.port.input.query.schooluser;

import java.util.UUID;

import com.sep.vox.domain.model.user.UserStatus;

public record ViewSchoolStudentsBySchoolQuery(
    UUID schoolId,
    int page,
    int size,
    String search,
    UserStatus status
) {

}
