package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;

final class SchoolUserStatusValidator {

    private SchoolUserStatusValidator() {
    }

    static void requireActive(User user) {
        requireActive(user, "Tài khoản người dùng không hợp lệ cho thao tác này");
    }

    static void requireActive(User user, String message) {
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(message);
        }
    }
}