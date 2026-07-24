package com.sep.vox.application.response.input.stream;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IssueStudentStreamTokenResponse(
    String token, 
    UUID scheduleId, 
    UUID sessionId, 
    List<String> streamTypes, 
    OffsetDateTime expiresAt
) {
    
}
