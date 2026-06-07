package com.sep.vox.application.mapper.schooluser;

import com.sep.vox.application.query.dto.SchoolUserInfo;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;

public class SchoolUserResponseMapper {

    public static SchoolUserResponse toResponse(User user, String roleCode, SchoolUser schoolUser) {
        return new SchoolUserResponse(
            user.getId(),
            user.getEmail() != null ? user.getEmail().value() : null,
            user.getPhone() != null ? user.getPhone().value() : null,
            user.getFullName() != null ? user.getFullName().value() : null,
            roleCode,
            user.getStatus() != null ? user.getStatus().name() : null,
            user.getSchoolId(),
            schoolUser != null ? schoolUser.getStudentId() : null,
            user.getCreatedAt(),
            user.getId(),
            schoolUser != null ? schoolUser.getStartDate() : null,
            schoolUser != null ? schoolUser.getEndDate() : null
        );
    }

    public static SchoolUserResponse toResponse(SchoolUserInfo info) {
        return new SchoolUserResponse(
            info.id(),
            info.email(),
            info.phone(),
            info.fullName(),
            info.roleCode(),
            info.status(),
            info.schoolId(),
            info.studentId(),
            info.createdAt(),
            info.userId(),
            info.startDate(),
            info.endDate()
        );
    }
}
