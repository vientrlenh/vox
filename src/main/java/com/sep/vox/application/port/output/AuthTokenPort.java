package com.sep.vox.application.port.output;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public interface AuthTokenPort {
    String generateJwtToken(String userId, @Nullable UUID schoolId, String email, List<String> roles);
    String getEmailFromToken(String token);
    UUID getUserIdFromToken(String token);
    @Nullable UUID getSchoolIdFromToken(String token);
}
