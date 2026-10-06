package com.sep.vox.application.port.output;


import com.sep.vox.application.response.output.AuthenticatedInfo;

public interface AuthenticationManagerPort {
    AuthenticatedInfo setAuthenticationAndGetInfo(String login, String password);
}
