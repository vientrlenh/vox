package com.sep.vox.application.port.input.query;

public record ViewGradeLevelsQuery(
        int page,
        int size,
        String search,
        String status
) {}
