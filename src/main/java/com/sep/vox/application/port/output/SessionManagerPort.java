package com.sep.vox.application.port.output;


import com.sep.vox.application.response.output.GeneratedSessionToken;

public interface SessionManagerPort {
    GeneratedSessionToken generateToken();
}
