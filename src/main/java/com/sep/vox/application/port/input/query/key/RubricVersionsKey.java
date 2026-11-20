package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record RubricVersionsKey(
        UUID rubricId,
        String status,
        int page,
        int size
) {}