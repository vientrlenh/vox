package com.sep.vox.application.response.input.assessmentsession;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamCandidateResultSectionResponse(
    UUID sectionId,
    String title,
    BigDecimal score
) {
}
