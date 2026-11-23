package com.sep.vox.application.port.input.query.rubricsystem;

import java.util.UUID;

public record SearchSystemRubricsQuery(
        String keyword,
        UUID frameworkId,
        UUID languageId,
        int page,
        int size
) {}