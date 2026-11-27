package com.sep.vox.application.query.dto;

/**
 * The number cards on the appeal screen.
 *
 * @param processing APPROVED + GRADING (the COMPARING status has been removed)
 * @param withdrawn  appeals the student withdrew themselves -- kept apart from {@code rejected}
 *                   because that is the student's decision, not the school's
 */
public record AppealStatsInfo(
    int pending,
    int processing,
    int published,
    int rejected,
    int withdrawn
) {
}
