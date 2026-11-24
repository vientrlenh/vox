package com.sep.vox.application.port.input.query.gradelevel;

import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;

public record ViewGradeLevelsQuery(
        int page,
        int size,
        String search,
        GradeLevelStatus status
) {}
