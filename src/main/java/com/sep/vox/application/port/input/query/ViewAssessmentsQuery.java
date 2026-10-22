package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;

public record ViewAssessmentsQuery(
    AssessmentKind kind,
    AssessmentStatus status,
    UUID schoolId,
    UUID schoolClassId,
    String keyword,
    AppPageRequest pageRequest
) {
}
