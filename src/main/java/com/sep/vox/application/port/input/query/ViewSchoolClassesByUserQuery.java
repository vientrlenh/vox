package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolClassesByUserQuery(
    UUID schoolId,
    UUID userId,
    String status,
    AppPageRequest pageRequest
) {

}
