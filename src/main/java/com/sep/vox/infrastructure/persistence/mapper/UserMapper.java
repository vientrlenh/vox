package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

public final class UserMapper {
    
    public static User toDomain(UserJpaEntity jpa) {
        return new User(
            jpa.getId(),
            new Email(jpa.getEmail()),
            jpa.getPasswordHash(),
            new Phone(jpa.getPhone()),
            new FullName(jpa.getFullName()),
            jpa.getGender() != null ? Gender.valueOf(jpa.getGender()) : null,
            jpa.getDateOfBirth(),
            jpa.getAddress(),
            UserStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy(),
            jpa.getSchoolId()
        );
    }

    public static UserJpaEntity toJpa(User user) {
        return new UserJpaEntity(
            user.getId(),  
            valueOf(user.getEmail()), 
            user.getPasswordHash(),  
            valueOf(user.getPhone()), 
            valueOf(user.getFullName()), 
            user.getGender() != null ? user.getGender().name() : null, 
            user.getDateOfBirth(), 
            user.getAddress(), 
            user.getStatus().name(), 
            user.getCreatedAt(), 
            user.getUpdatedAt(), 
            user.getCreatedBy(), 
            user.getUpdatedBy(),
            user.getSchoolId()
        );
    }

    private static String valueOf(Email email) {
        return email == null ? null : email.value();
    }

    private static String valueOf(Phone phone) {
        return phone == null ? null : phone.value();
    }

    private static String valueOf(FullName fullName) {
        return fullName == null ? null : fullName.value();
    }
}
