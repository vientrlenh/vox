package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

public class UserMapper {
    
    public static User toDomain(UserJpaEntity jpa) {
        return new User(
            jpa.getId(),
            new Email(jpa.getEmail()),
            jpa.getPasswordHash(),
            new Phone(jpa.getPhone()),
            jpa.getFullName(),
            jpa.getGender() != null ? Gender.valueOf(jpa.getGender()) : null,
            jpa.getDateOfBirth(),
            jpa.getAddress(),
            UserStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static UserJpaEntity toJpa(User user) {
        return new UserJpaEntity(
            user.getId(),  
            user.getEmail().value(), 
            user.getPasswordHash(),  
            user.getPhone().value(), 
            user.getFullName(), 
            user.getGender() != null ? user.getGender().name() : null, 
            user.getDateOfBirth(), 
            user.getAddress(), 
            user.getStatus().name(), 
            user.getCreatedAt(), 
            user.getUpdatedAt(), 
            user.getCreatedBy(), 
            user.getUpdatedBy()
        );
    }
}
