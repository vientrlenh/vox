package com.sep.vox.interfaces.graphql.dto.request;

public record UpdateSchoolGradeLevelRequest(
        String name,
        String description,
        Integer order
) {}
