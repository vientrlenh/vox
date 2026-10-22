package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolDebtEventsQuery(
    UUID schoolId,
    AppPageRequest pageRequest
) {
}
