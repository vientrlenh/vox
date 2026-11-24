package com.sep.vox.application.port.input.query.rubricteacher;

import java.util.UUID;

public record ViewTeacherRubricVersionsQuery(
        UUID rubricId,
        int page,
        int size
) {}