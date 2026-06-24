package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolUsersBySchoolQuery(
    UUID schoolId,
    int page,
    int size,
    String search,
    String role,
    String status
) {

}
