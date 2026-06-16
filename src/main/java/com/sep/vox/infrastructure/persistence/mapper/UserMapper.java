package com.sep.vox.infrastructure.persistence.mapper;

import java.time.LocalDate;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.DateOfBirth;
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
            gender(jpa.getGender()),
            new DateOfBirth(jpa.getDateOfBirth()),
            jpa.getAddress(),
            jpa.getAvatarUrl(),
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
            valueOf(user.getEmail()), 
            user.getPasswordHash(),  
            valueOf(user.getPhone()), 
            valueOf(user.getFullName()), 
            genderValue(user.getGender()), 
            valueOf(user.getDateOfBirth()), 
            user.getAddress(), 
            user.getAvatarUrl(),
            user.getStatus().name(), 
            user.getCreatedAt(), 
            user.getUpdatedAt(), 
            user.getCreatedBy(), 
            user.getUpdatedBy()
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

    private static LocalDate valueOf(DateOfBirth dateOfBirth) {
        return dateOfBirth == null ? null : dateOfBirth.value();
    }

    private static String genderValue(Gender gender) {
        return gender == null ? null : gender.name();
    }

    private static Gender gender(String genderValue) {
        return genderValue == null ? null : Gender.valueOf(genderValue);
    }
}
