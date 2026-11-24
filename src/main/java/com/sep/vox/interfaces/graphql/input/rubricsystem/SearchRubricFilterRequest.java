package com.sep.vox.interfaces.graphql.input.rubricsystem;

import java.util.UUID;

public record SearchRubricFilterRequest(
        String keyword,
        UUID frameworkId,
        UUID languageId
) {}