package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolRubricsQuery(
        UUID schoolId,
        int page,
        int size
) {}