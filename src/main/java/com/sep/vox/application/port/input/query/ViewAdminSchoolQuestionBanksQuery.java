package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAdminSchoolQuestionBanksQuery(
    UUID schoolId,
    int page,
    int size
) {
}
