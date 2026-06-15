package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.rubric.RubricStatus;

import java.util.UUID;

public record ViewSystemRubricVersionsQuery(
        UUID rubricId,
        String status, // Sửa thành String để hứng từ GraphQL cho an toàn
        int page,
        int size
) {}