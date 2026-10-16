package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record CanViewAssessmentBlueprintDataQuery(UUID assessmentSchoolId, String assessmentStatus) {
}
