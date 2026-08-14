package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolDebtEventsQuery(
    UUID schoolId,
    int page,
    int size
) {
}
