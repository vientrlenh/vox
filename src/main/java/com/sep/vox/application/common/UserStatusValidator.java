package com.sep.vox.application.common;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;

public final class UserStatusValidator {

    private UserStatusValidator() {
    }

    public static void requireActive(User user) {
        requireActive(user, "Tài khoản người dùng không hợp lệ cho thao tác này");
    }

    public static void requireActive(User user, String message) {
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(message);
        }
    }

    public static void requireActiveTarget(User user) {
        requireActive(user, "Người dùng không còn hoạt động");
    }

}