package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.assessment.AssessmentStatus;
import java.util.UUID;

public record CanViewAssessmentBlueprintDataQuery(UUID assessmentSchoolId, AssessmentStatus assessmentStatus) {
}
