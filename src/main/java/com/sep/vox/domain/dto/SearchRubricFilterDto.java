package com.sep.vox.domain.dto;

import java.util.UUID;

public record SearchRubricFilterDto(
        String keyword,
        UUID frameworkId,
        UUID languageId
) {}