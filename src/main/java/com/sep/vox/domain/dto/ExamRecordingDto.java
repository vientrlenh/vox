package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamRecordingDto(
    UUID id, 
    UUID examSessionId, 
    UUID candidateId, 
    String streamType, 
    String status, 
    Long sizeBytes, 
    Integer durationSeconds, 
    String createdAt, 
    String assembledAt
) {
    
}
