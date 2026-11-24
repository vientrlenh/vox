package com.sep.vox.application.port.input.query.rubricteacher;

import java.util.UUID;

public record SearchTeacherRubricsQuery(
        String keyword,
        UUID frameworkId,
        UUID languageId,
        int page,
        int size
) {}
