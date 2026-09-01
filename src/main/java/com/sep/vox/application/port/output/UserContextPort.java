package com.sep.vox.application.port.output;

import java.util.Optional;
import java.util.UUID;

public interface UserContextPort {
    UUID getCurrentAuthenticatedUserId();
    Optional<UUID> findCurrentAuthenticatedUserId();
    boolean isSystemAdmin();
    UUID getCurrentSchoolId();
    boolean isSchoolAdmin();
    boolean isTeacher();
}
