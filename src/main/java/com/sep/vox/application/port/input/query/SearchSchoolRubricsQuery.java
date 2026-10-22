package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;


public record SearchSchoolRubricsQuery(
        UUID schoolId,
        String keyword,
        UUID frameworkId,
        UUID languageId,
        AppPageRequest pageRequest
) {}