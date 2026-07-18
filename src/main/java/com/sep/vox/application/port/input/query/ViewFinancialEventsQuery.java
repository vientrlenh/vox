package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewFinancialEventsQuery(
    UUID schoolId,
    int page,
    int size
) {
}
