package com.sep.vox.application.response.input.assessmentsession;

import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentCandidateResultSectionResponse(
    UUID sectionId,
    String title,
    BigDecimal score
) {
}
