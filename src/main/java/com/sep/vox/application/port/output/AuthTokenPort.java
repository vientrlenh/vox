package com.sep.vox.application.port.output;

import java.util.UUID;

public interface AuthTokenPort {
    String generateJwtToken(String userId, String email, String role, String type);
    String getEmailFromToken(String token, String type);
    UUID getUserIdFromToken(String token, String type);
}
