package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record SearchSchoolRubricCriteriaQuery(
        UUID schoolId,
        UUID versionId,
        String keyword,
        Boolean isRequired,
        AppPageRequest pageRequest
) {}