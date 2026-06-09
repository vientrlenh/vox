package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewTeacherBankTopicsQuery(
    UUID bankId,
    int page,
    int size
) {
}
