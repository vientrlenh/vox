package com.sep.vox.application.port.input.query.rubricschool;

import java.util.UUID;


public record SearchSchoolRubricsQuery(
        UUID schoolId,
        String keyword,
        UUID frameworkId,
        UUID languageId,
        int page,
        int size
) {}