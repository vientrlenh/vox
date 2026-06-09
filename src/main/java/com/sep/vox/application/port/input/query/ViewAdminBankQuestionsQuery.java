package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAdminBankQuestionsQuery(
    UUID bankId,
    int page,
    int size,
    Boolean includeArchived,
    String status,
    String keyword
) {
}
