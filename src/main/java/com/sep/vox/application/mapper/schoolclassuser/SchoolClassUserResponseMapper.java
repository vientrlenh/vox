package com.sep.vox.application.mapper.schoolclassuser;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.response.input.schoolclassuser.SchoolClassUserResponse;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public final class SchoolClassUserResponseMapper {

    private SchoolClassUserResponseMapper() {
    }

    public static SchoolClassUserResponse toResponse(SchoolClassUser schoolClassUser, Map<UUID, User> usersById) {
        return new SchoolClassUserResponse(
            schoolClassUser.getId(),
            schoolClassUser.getUserId(),
            schoolClassUser.getSchoolClassId(),
            schoolClassUser.isActive(),
            valueOf(schoolClassUser.getJoinedAt()),
            valueOf(schoolClassUser.getLeftAt()),
            schoolClassUser.getAssignedBy(),
            toUserDto(usersById.get(schoolClassUser.getUserId()))
        );
    }

    private static UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
            user.getId(),
            valueOf(user.getEmail()),
            valueOf(user.getPhone()),
            valueOf(user.getFullName()),
            valueOf(user.getGender()),
            valueOf(user.getDateOfBirth()),
            user.getAddress(),
            user.getAvatarUrl(),
            valueOf(user.getCreatedAt()),
            valueOf(user.getUpdatedAt())
        );
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
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

    private static String valueOf(DateOfBirth dateOfBirth) {
        return dateOfBirth == null ? null : dateOfBirth.toString();
    }
}
