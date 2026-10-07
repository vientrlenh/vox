package com.sep.vox.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.BirthDate;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.Phone;

public record UserDto(
    UUID id, 
    String email, 
    String phone, 
    String fullName, 
    Gender gender, 
    LocalDate birthDate, 
    String address, 
    String avatarUrl, 
    Instant createdAt, 
    Instant updatedAt 
) {

    public static UserDto toDto(User user) {
        return new UserDto(
            user.getId(), 
            Email.valueOf(user.getEmail()),
            Phone.valueOf(user.getPhone()),
            Name.valueOf(user.getFullName()),
            user.getGender(), 
            BirthDate.valueOf(user.getBirthDate()), 
            user.getAddress(), 
            user.getAvatarUrl(), 
            user.getCreatedAt(), 
            user.getUpdatedAt()
        );
    }

    public static List<UserDto> toDtoList(List<User> users) {
        return users.stream()
            .map(UserDto::toDto)
            .toList();
    }

    public static PageResult<UserDto> toDtoPage(PageResult<User> userPage) {
        return new PageResult<>(
            toDtoList(userPage.content()), 
            userPage.page(), 
            userPage.size(), 
            userPage.totalElements(), 
            userPage.totalPages()
        );
    }
}
