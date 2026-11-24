package com.sep.vox.interfaces.graphql.input.rubricsystem;

public record SearchRubricCriterionFilterRequest(
        String keyword,
        Boolean isRequired
) {}