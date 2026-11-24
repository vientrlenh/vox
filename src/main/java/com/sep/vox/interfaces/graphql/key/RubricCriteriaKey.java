package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;

public record RubricCriteriaKey(
        UUID versionId,
        int page,
        int size
) {}