package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.infrastructure.persistence.entity.registerform.RegisterFormDocumentJpaEntity;

public final class RegisterFormDocumentMapper {
    
    public static RegisterFormDocument toDomain(RegisterFormDocumentJpaEntity jpa) {
        return RegisterFormDocument.builder()
            .id(jpa.getId())
            .registerFormId(jpa.getRegisterFormId())
            .url(jpa.getUrl())
            .createdAt(jpa.getCreatedAt())
            .build();
    }

    public static RegisterFormDocumentJpaEntity toJpa(RegisterFormDocument document) {
        return RegisterFormDocumentJpaEntity.builder()
            .id(document.getId())
            .registerFormId(document.getRegisterFormId())
            .url(document.getUrl())
            .createdAt(document.getCreatedAt())
            .build();
    }
}
