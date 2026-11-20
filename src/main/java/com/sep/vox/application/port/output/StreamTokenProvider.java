package com.sep.vox.application.port.output;

import java.time.Instant;
import java.util.List;

public interface StreamTokenProvider {
    String generateStreamToken(String userId, String candidateId, String scheduleId, String examId, String sessionId, List<String> streamTypes, Instant windowStart, Instant windowEnd);
    
    String generateMonitorToken(String userId, String schoolId, String examId, String monitorScope, List<String> scheduleIds, List<String> roles, Instant windowStart, Instant windowEnd);
}
