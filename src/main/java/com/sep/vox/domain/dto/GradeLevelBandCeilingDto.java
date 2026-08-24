package com.sep.vox.domain.dto;

public record GradeLevelBandCeilingDto(
    FrameworkResultBandDto defaultBand,
    FrameworkResultBandDto hardMaxBand
) {
}
