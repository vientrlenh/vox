package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.infrastructure.persistence.entity.RegisterFormJpaEntity;

public class RegisterFormMapper {
    
    public static RegisterForm toDomain(RegisterFormJpaEntity jpa) {
        return new RegisterForm(
            jpa.getId(), 
            jpa.getContactFullName(), 
            jpa.getIdentityNumber(), 
            jpa.getContactPhone(), 
            jpa.getContactEmail(), 
            jpa.getSchoolDomain(),
            jpa.getSchoolName(), 
            jpa.getSchoolAddress(), 
            jpa.getPostalCode(), 
            jpa.getPosition(), 
            jpa.getStudentCount(), 
            jpa.getReason(), 
            RegisterFormStatus.valueOf(jpa.getStatus()), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(),
            jpa.getUpdatedBy()
        );
    }

    public static RegisterFormJpaEntity toJpa(RegisterForm rf) {
        return new RegisterFormJpaEntity(
            rf.getId(), 
            rf.getContactFullName(), 
            rf.getIdentityNumber(), 
            rf.getContactPhone(), 
            rf.getContactEmail(), 
            rf.getSchoolDomain(), 
            rf.getSchoolName(), 
            rf.getSchoolAddress(), 
            rf.getPostalCode(), 
            rf.getPosition(), 
            rf.getStudentCount(), 
            rf.getReason(), 
            rf.getStatus().name(), 
            rf.getCreatedAt(), 
            rf.getUpdatedAt(), 
            rf.getUpdatedBy()
        );
    }
}
