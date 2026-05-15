package com.sep.vox.application.port.output;

import java.util.List;
import java.util.UUID;

public interface AuthTokenPort {
    String generateJwtToken(String userId, List<String> roles);
    String getEmailFromToken(String token);
    UUID getUserIdFromToken(String token);
}
