package com.sep.vox.interfaces.graphql.mapper;

public record UpdateFrameworkCriterionInput(
    String code,
    String name,
    String description,
    int order
) {}
