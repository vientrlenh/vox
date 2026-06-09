package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAdminBankTopicsQuery(
    UUID bankId,
    int page,
    int size,
    Boolean includeArchived
) {
}
