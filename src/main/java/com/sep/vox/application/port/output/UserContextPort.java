package com.sep.vox.application.port.output;

import java.util.UUID;

public interface UserContextPort {
    UUID getCurrentAuthenticatedUserId();
    boolean isSystemAdmin();
}
