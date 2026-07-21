package com.sep.vox.application.query.dto;

public record AppealStatsInfo(
    int pending,
    int processing,
    int published,
    int rejected
) {
}
