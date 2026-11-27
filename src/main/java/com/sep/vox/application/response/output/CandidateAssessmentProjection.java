package com.sep.vox.application.response.output;

import java.time.Instant;
import java.util.UUID;

/** A candidate of a centralized assessment, used to pick the assessment nearest to the current time. */
public record CandidateAssessmentProjection(
    UUID id, 
    String code, 
    String name, 
    String status, 
    Instant openAt, 
    Instant closeAt) {
}
