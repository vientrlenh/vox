package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolRubricVersionsQuery(
        UUID schoolId,
        UUID rubricId,
        String status,
        AppPageRequest pageRequest
) {}