package com.sep.vox.interfaces.graphql.dto.request;

public record SearchRubricVersionFilterRequest(
        String keyword,
        String status
) {}