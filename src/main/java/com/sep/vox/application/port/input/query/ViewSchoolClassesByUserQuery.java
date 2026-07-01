package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolClassesByUserQuery(
    UUID schoolId,
    UUID userId,
    int page,
    int size
) {

}
