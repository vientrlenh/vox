package com.sep.vox.application.port.output;

import java.util.UUID;

public interface AuthenticationManagerPort {
    String setAuthenticationAndGetUserEmail(String login, String password);
    UUID setAuthenticationAndGetUserId(String login, String password);
}
