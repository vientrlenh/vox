package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolTeachersBySchoolQuery(
    UUID schoolId,
    int page,
    int size,
    String search,
    String status
) {

}
