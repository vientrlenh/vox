package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterFormDocumentDto(
    UUID id, 
    UUID registerFormId, 
    String url, 
    Instant createdAt
) {
    
}
