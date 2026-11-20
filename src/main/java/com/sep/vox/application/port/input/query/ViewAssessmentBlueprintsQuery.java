package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import java.util.UUID;

public record ViewAssessmentBlueprintsQuery(
    UUID schoolId,
    Boolean isActive,
    UUID languageId,
    AssessmentKind assessmentKind,
    String keyword,
    int page,
    int size
) {
}
