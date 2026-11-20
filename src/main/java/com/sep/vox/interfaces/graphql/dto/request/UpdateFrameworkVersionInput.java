package com.sep.vox.interfaces.graphql.dto.request;

import java.time.Instant;

public record UpdateFrameworkVersionInput(
    String code,
    String name,
    String description,
    Instant effectiveFrom,
    Instant effectiveTo
) {}
