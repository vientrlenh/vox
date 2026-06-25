package com.sep.vox.domain.dto;

import java.util.UUID;

public record RegisterFormDocumentDto(
    UUID id, 
    UUID registerFormId, 
    String url, 
    String createdAt
) {
    
}
