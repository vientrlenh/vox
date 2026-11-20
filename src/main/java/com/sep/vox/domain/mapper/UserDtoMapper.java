package com.sep.vox.domain.mapper;

import java.time.LocalDate;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.valueobject.DateOfBirth;
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
            user.getGender(),
            valueOf(user.getDateOfBirth()),
            user.getAddress(), 
            user.getAvatarUrl(), 
            user.getCreatedAt(), 
            user.getUpdatedAt()
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

    /**
     * The date of birth CAN be null: the column is nullable, and UpdateSchoolUserUseCase lets a school
     * admin blank out the date of birth of a user in the school. This used to call
     * {@code getDateOfBirth().value()} directly, so a single person with an empty date of birth made the
     * whole profile READ path throw an NPE -- profile, user(id), users, and also the updateProfile response.
     */
    private static LocalDate valueOf(DateOfBirth dateOfBirth) {
        return dateOfBirth == null ? null : dateOfBirth.value();
    }
}
