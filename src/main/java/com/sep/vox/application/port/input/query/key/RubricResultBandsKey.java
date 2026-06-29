package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record RubricResultBandsKey(
        UUID versionId,
        int page,
        int size
) {}