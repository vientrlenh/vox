package com.sep.vox.domain.dto;

import java.util.UUID;

public record FrameworkResultBandDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String label,
    String description,
    int order
) {
}
