package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewQuestionTopicsQuery(
    UUID bankId,
    int page,
    int size
) {
}
