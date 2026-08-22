package com.sep.vox.interfaces.graphql.dto.request;

public record UpdateGradeLevelRequest(
        String name,
        String description,
        Integer order
) {}
