package com.sep.vox.interfaces.graphql.dto.request;

public record SearchRubricCriterionFilterRequest(
        String keyword,
        Boolean isRequired
) {}