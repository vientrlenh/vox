package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewInvoicesQuery(
    UUID schoolId,
    int page,
    int size
) {
}
