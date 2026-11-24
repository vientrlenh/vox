package com.sep.vox.application.port.input.query.rubricsystem;

import java.util.UUID;

public record SearchSystemRubricResultBandsQuery(
        UUID versionId,
        String keyword,
        int page,
        int size
) {
}