package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

public record SearchRubricFilterRequest(
        String keyword,
        UUID frameworkId,
        UUID languageId
) {}