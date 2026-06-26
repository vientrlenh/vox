package com.sep.vox.interfaces.graphql.dto.request;

public record SearchRubricCriterionFilterDto(
        String keyword,
        Boolean isRequired
) {}