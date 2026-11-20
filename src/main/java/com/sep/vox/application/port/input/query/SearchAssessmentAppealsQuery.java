package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;

public record SearchAssessmentAppealsQuery(
    AssessmentAppealStatus status,
    String keyword,
    int page,
    int size
) {
}
