package com.sep.vox.application.port.output;

import java.util.List;
import java.util.UUID;


public interface AuthTokenPort {
    String generateToken(UUID userId, UUID schoolId, String email, List<String> roles);
    String extractEmail(String token);
    UUID extractUserId(String token);
    UUID extractSchoolId(String token);
}
