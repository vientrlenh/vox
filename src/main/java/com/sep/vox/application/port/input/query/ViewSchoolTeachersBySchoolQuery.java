package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolTeachersBySchoolQuery(
    UUID schoolId,
    AppPageRequest pageRequest,
    String search,
    String status
) {

}
