package com.sep.vox.application.port.input.query;

import com.sep.vox.application.shared.AppPageRequest;

public record SearchAssessmentAppealsQuery(
    String status,
    String keyword,
    AppPageRequest pageRequest
) {
}
