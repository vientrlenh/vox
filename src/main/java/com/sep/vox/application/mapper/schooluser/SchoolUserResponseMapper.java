package com.sep.vox.application.mapper.schooluser;

import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.model.schooluser.SchoolUser;
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
            user.getCreatedAt()
        );
    }
}
