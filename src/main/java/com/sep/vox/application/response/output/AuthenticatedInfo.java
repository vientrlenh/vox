package com.sep.vox.application.response.output;

import java.util.UUID;

public record AuthenticatedInfo(
    UUID userId, 
    String email
) {
    
}
