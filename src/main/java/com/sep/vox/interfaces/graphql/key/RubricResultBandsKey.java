package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;

public record RubricResultBandsKey(
        UUID versionId,
        int page,
        int size
) {}