package com.sep.vox.infrastructure.persistence.mapper;

import java.time.LocalDate;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.infrastructure.persistence.entity.user.UserJpaEntity;

public final class UserMapper {
    
    public static User toDomain(UserJpaEntity jpa) {
        return User.builder()
            .id(jpa.getId())
            .email(new Email(jpa.getEmail()))
            .passwordHash(jpa.getPasswordHash())
            .phone(new Phone(jpa.getPhone()))
            .fullName(new FullName(jpa.getFullName()))
            .gender(gender(jpa.getGender()))
            .dateOfBirth(new DateOfBirth(jpa.getDateOfBirth()))
            .address(jpa.getAddress())
            .avatarUrl(jpa.getAvatarUrl())
            .status(UserStatus.valueOf(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static UserJpaEntity toJpa(User user) {
        return UserJpaEntity.builder()
            .id(user.getId())
            .email(valueOf(user.getEmail()))
            .passwordHash(user.getPasswordHash())
            .phone(valueOf(user.getPhone()))
            .fullName(valueOf(user.getFullName()))
            .gender(genderValue(user.getGender()))
            .dateOfBirth(valueOf(user.getDateOfBirth()))
            .address(user.getAddress())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus().name())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .createdBy(user.getCreatedBy())
            .updatedBy(user.getUpdatedBy())
            .build();
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
