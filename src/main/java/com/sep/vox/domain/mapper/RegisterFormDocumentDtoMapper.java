package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.RegisterFormDocumentDto;
import com.sep.vox.domain.model.registerform.RegisterFormDocument;

public final class RegisterFormDocumentDtoMapper {
    
    public static RegisterFormDocumentDto toRegisterFormDocumentDto(RegisterFormDocument document) {
        return new RegisterFormDocumentDto(
            document.getId(), 
            document.getRegisterFormId(), 
            document.getUrl(), 
            document.getCreatedAt().toString()
        );
    }
}
