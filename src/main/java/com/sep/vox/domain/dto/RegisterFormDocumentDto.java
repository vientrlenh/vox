package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.registerform.RegisterFormDocument;

public record RegisterFormDocumentDto(
    UUID id,
    UUID registerFormId,
    String url,
    Instant createdAt
) {

    public static RegisterFormDocumentDto toDto(RegisterFormDocument registerFormDocument) {
        return new RegisterFormDocumentDto(
            registerFormDocument.getId(),
            registerFormDocument.getRegisterFormId(),
            registerFormDocument.getUrl(),
            registerFormDocument.getCreatedAt()
        );
    }
}
