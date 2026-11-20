package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import java.time.Instant;
import java.util.UUID;

public record AssessmentRecordingDto(
    UUID id, 
    UUID assessmentSessionId, 
    UUID candidateId, 
    AssessmentRequiredStreamType streamType, 
    AssessmentRecordingAssemblyStatus status, 
    Long sizeBytes,
    Integer durationSeconds,
    String source,
    // The recording regarded as canonical for this streamType. Every ingest source keeps its own row, so one
    // assessment session can have several recordings of the same kind; this flag says which one to open first without
    // hiding the others -- the WebRTC recording is the only one that does not pass through the candidate's machine, so it
    // must always stay reachable even when it is not the canonical one.
    boolean canonical,
    Instant createdAt,
    Instant assembledAt
) {
    
}
