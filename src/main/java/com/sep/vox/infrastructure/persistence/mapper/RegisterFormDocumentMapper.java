package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.infrastructure.persistence.entity.RegisterFormDocumentJpaEntity;

public final class RegisterFormDocumentMapper {
    
    public static RegisterFormDocument toDomain(RegisterFormDocumentJpaEntity jpa) {
        return new RegisterFormDocument(
            jpa.getId(), 
            jpa.getRegisterFormId(), 
            jpa.getUrl(), 
            jpa.getCreatedAt()
        );
    }

    public static RegisterFormDocumentJpaEntity toJpa(RegisterFormDocument document) {
        return new RegisterFormDocumentJpaEntity(
            document.getId(), 
            document.getRegisterFormId(), 
            document.getUrl(), 
            document.getCreatedAt()
        );
    }
}
