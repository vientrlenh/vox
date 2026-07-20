package com.sep.vox.application.port.output;

import java.time.OffsetDateTime;
import java.util.List;

public interface StreamTokenProvider {
    String generateStreamToken(String userId, String candidateId, String scheduleId, String examId, String sessionId, List<String> streamTypes, OffsetDateTime windowStart, OffsetDateTime windowEnd);
}
