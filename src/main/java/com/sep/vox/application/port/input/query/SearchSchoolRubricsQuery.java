package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.common.PageRequest;
import java.util.UUID;

public record SearchSchoolRubricsQuery(
        UUID schoolId,
        String keyword,
        UUID frameworkId,
        UUID languageId,
        PageRequest pageRequest
) {}