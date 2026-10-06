package com.sep.vox.application.port.output;

import java.util.Optional;
import java.util.UUID;

public interface UserContextPort {
    Optional<UUID> getCurrentAuthenticatedUserId();
    boolean isSystemAdmin();
    Optional<UUID> getCurrentSchoolId();
    boolean isSchoolAdmin();
    boolean isTeacher();
}
