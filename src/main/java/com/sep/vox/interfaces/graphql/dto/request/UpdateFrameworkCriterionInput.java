package com.sep.vox.interfaces.graphql.dto.request;

public record UpdateFrameworkCriterionInput(
    String code,
    String name,
    String description,
    int order
) {}
