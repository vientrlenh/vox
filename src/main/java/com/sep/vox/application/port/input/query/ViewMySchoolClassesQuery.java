package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewMySchoolClassesQuery(
    UUID schoolId,
    String status,
    int page,
    int size
) {

}
