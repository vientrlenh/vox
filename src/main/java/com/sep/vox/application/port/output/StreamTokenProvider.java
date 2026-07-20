package com.sep.vox.application.port.output;

import java.time.OffsetDateTime;
import java.util.List;

public interface StreamTokenProvider {
    String generateToken(String userId, List<String> scheduleIds, String examId, String sessionId, List<String> roles, List<String> streamTypes, OffsetDateTime windowStart, OffsetDateTime windowEnd);
}
