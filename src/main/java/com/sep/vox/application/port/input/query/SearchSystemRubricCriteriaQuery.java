package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record SearchSystemRubricCriteriaQuery(
        UUID versionId,
        String keyword,
        Boolean isRequired,
        AppPageRequest pageRequest
) {}