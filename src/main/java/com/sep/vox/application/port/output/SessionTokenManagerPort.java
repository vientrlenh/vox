package com.sep.vox.application.port.output;



import com.sep.vox.application.response.output.GeneratedSessionToken;

public interface SessionTokenManagerPort {
    GeneratedSessionToken generateToken();
    String hash(String token);
}
