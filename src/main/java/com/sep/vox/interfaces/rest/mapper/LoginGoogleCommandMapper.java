package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.LoginGoogleCommand;
import com.sep.vox.interfaces.rest.dto.request.LoginGoogleRequest;

public final class LoginGoogleCommandMapper {

    // Private constructor để ngăn không cho khởi tạo object từ class này (vì toàn hàm static)
    private LoginGoogleCommandMapper() {}

    public static LoginGoogleCommand fromRequest(LoginGoogleRequest request, String ipAddress, String userAgent) {
        return new LoginGoogleCommand(
                request.idToken(),
                ipAddress,
                userAgent,
                ClientDeviceCommandMapper.fromRequest(request.clientDevice())
        );
    }
}