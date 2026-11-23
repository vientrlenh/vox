package com.sep.vox.application.port.input.query.balance;

import java.util.UUID;

public record ViewSchoolDebtEventsQuery(
    UUID schoolId,
    int page,
    int size
) {
}
