package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record SearchSchoolRubricResultBandsQuery(
        UUID schoolId,
        UUID versionId,
        String keyword,
        int page,
        int size
) {
}