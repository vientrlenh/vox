package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record SearchSystemRubricVersionsQuery(
        UUID rubricId,
        String keyword,
        String status,
        AppPageRequest pageRequest
) {}