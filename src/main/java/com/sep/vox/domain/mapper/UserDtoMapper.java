package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public final class UserDtoMapper {
    
    public static UserDto toUserDto(User user) {
        return new UserDto(
            user.getId(), 
            valueOf(user.getEmail()), 
            valueOf(user.getPhone()), 
            valueOf(user.getFullName()), 
            valueOf(user.getGender()), 
            user.getDateOfBirth().value().toString(),
            user.getAddress(), 
            user.getAvatarUrl(), 
            user.getCreatedAt().toString(), 
            user.getUpdatedAt().toString()
        );
    }

    public static List<UserDto> toUserDtoList(List<User> users) {
        return users.stream()
            .map(UserDtoMapper::toUserDto)
            .toList();
    }

    public static PageResult<UserDto> toUserDtoPage(PageResult<User> userPage) {
        return new PageResult<>(
            toUserDtoList(userPage.content()), 
            userPage.page(), 
            userPage.size(), 
            userPage.totalElements(), 
            userPage.totalPages()
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

    private static String valueOf(Gender gender) {
        return gender == null ? null : gender.name();
    }
}
