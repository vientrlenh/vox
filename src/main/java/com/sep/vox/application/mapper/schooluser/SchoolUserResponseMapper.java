package com.sep.vox.application.mapper.schooluser;

import com.sep.vox.application.query.dto.SchoolUserInfo;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;

public class SchoolUserResponseMapper {

    public static SchoolUserResponse toResponse(User user, String roleCode, SchoolUser schoolUser) {
        return new SchoolUserResponse(
            schoolUser != null ? schoolUser.getId() : null,
            schoolUser != null ? schoolUser.getSchoolId() : null,
            user.getId(),
            roleCode,
            schoolUser != null ? schoolUser.getStartDate() : null,
            schoolUser != null ? schoolUser.getEndDate() : null
        );
    }

    public static SchoolUserResponse toResponse(SchoolUserInfo info) {
        return new SchoolUserResponse(
            info.id(),
            info.schoolId(),
            info.userId(),
            info.roleCode(),
            info.startDate(),
            info.endDate()
        );
    }
}
