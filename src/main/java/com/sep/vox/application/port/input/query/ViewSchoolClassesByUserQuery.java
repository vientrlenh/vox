package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolClassesByUserQuery(
    UUID schoolId,
    UUID userId,
    String status,
    int page,
    int size
) {

}
