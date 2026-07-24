package com.sep.vox.interfaces.graphql.dto.request;

public record UpdateFrameworkVersionInput(
    String code,
    String name,
    String description,
    String effectiveFrom,
    String effectiveTo
) {}
