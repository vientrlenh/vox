package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;

/** The appeals of ONE assessment — the screen of the chair of that assessment. */
public record SearchAppealsByAssessmentQuery(
    UUID assessmentId,
    AssessmentAppealStatus status,
    String keyword,
    int page,
    int size
) {
}
