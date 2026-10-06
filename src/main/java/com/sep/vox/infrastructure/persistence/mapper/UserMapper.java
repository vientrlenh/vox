package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.BirthDate;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class UserMapper {
    
    public static User toDomain(UserJpaEntity jpa) {
        try {
            return User.builder()
                .id(jpa.getId())
                .email(Email.from(jpa.getEmail()))
                .passwordHash(jpa.getPasswordHash())
                .role(UserRole.from(jpa.getRole()))
                .phone(Phone.from(jpa.getPhone()))
                .fullName(Name.from(jpa.getFullName()))
                .gender(Gender.from(jpa.getGender()))
                .birthDate(BirthDate.from(jpa.getBirthDate()))
                .address(jpa.getAddress())
                .avatarUrl(jpa.getAvatarUrl())
                .status(UserStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred while converting user entity to user model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("An invalid state happen in user entity");
        }

    }

    public static UserJpaEntity toJpa(User user) {
        try {
            return UserJpaEntity.builder()
                .id(user.getId())
                .email(Email.valueOf(user.getEmail()))
                .passwordHash(user.getPasswordHash())
                .role(UserRole.value(user.getRole()))
                .phone(Phone.valueOf(user.getPhone()))
                .fullName(Name.valueOf(user.getFullName()))
                .gender(Gender.value(user.getGender()))
                .birthDate(BirthDate.valueOf(user.getBirthDate()))
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .status(UserStatus.value(user.getStatus()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred while converting user model to user entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("An invalid state happen in user model");
        }

    }

    
}
