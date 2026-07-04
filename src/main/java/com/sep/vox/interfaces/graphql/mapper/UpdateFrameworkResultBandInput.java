package com.sep.vox.interfaces.graphql.mapper;

public record UpdateFrameworkResultBandInput(
    String code,
    String label,
    String description,
    int order
) {}
