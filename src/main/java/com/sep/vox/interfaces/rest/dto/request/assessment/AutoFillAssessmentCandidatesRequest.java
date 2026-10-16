package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;
import java.util.UUID;

public record AutoFillAssessmentCandidatesRequest(
    List<UUID> scheduleIds
) {
}
