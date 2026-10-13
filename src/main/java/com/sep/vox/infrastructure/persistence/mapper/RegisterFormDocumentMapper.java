package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.infrastructure.persistence.entity.RegisterFormDocumentJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RegisterFormDocumentMapper {

    private RegisterFormDocumentMapper() {}

    public static RegisterFormDocument toDomain(RegisterFormDocumentJpaEntity jpa) {
        try {
            return RegisterFormDocument.builder()
                .id(jpa.getId())
                .registerFormId(jpa.getRegisterFormId())
                .url(jpa.getUrl())
                .createdAt(jpa.getCreatedAt())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping register form document entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static RegisterFormDocumentJpaEntity toJpa(RegisterFormDocument document) {
        try {
            return RegisterFormDocumentJpaEntity.builder()
                .id(document.getId())
                .registerFormId(document.getRegisterFormId())
                .url(document.getUrl())
                .createdAt(document.getCreatedAt())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping register form document model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
