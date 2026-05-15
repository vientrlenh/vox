package com.sep.vox.application.port.output;

import java.util.List;
import java.util.UUID;

public interface AuthTokenPort {
    String generateJwtToken(String userId, String email, List<String> roles, String type);
    String getEmailFromToken(String token, String type);
    UUID getUserIdFromToken(String token, String type);
}
