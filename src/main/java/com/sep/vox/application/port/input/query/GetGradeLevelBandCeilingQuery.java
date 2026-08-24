package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record GetGradeLevelBandCeilingQuery(
        UUID schoolId,
        UUID frameworkVersionId,
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId
) {}
