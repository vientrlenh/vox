package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.business.Email;
import com.sep.vox.domain.valueobject.business.Phone;
import com.sep.vox.domain.valueobject.id.UserId;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

public class UserMapper {
    
    public static User toDomain(UserJpaEntity jpa) {
        return new User(
            new UserId(jpa.getId()),
            new Email(jpa.getEmail()),
            jpa.getPasswordHash(),
            new Phone(jpa.getPhone()),
            jpa.getFullName(),
            Gender.valueOf(jpa.getGender()),
            jpa.getDateOfBirth(),
            jpa.getAddress(),
            UserStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            new UserId(jpa.getCreatedBy()),
            new UserId(jpa.getUpdatedBy())
        );
    }

    public static UserJpaEntity toJpa(User user) {
        return new UserJpaEntity(
            user.getId().value(),  
            user.getEmail().value(), 
            user.getPasswordHash(),  
            user.getPhone().value(), 
            user.getFullName(), 
            user.getGender().name(), 
            user.getDateOfBirth(), 
            user.getAddress(), 
            user.getStatus().name(), 
            user.getCreatedAt(), 
            user.getUpdatedAt(), 
            user.getCreatedBy().value(), 
            user.getUpdatedBy().value()
        );
    }
}
