package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSystemRubricVersionsQuery(
        UUID rubricId,
        String status, // Sửa thành String để hứng từ GraphQL cho an toàn
        AppPageRequest pageRequest
) {}