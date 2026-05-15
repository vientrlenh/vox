package com.sep.vox.application.port.output;

public interface AuthenticationManagerPort {
    String setAuthenticationAndGetUserEmail(String login, String password);
}
