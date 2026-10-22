package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolStudentsBySchoolQuery(
    UUID schoolId,
    AppPageRequest pageRequest,
    String search,
    String status
) {

}
