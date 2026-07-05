package com.sep.vox.application.port.output;

import java.time.OffsetDateTime;
import java.util.List;

public interface StreamTokenProvider {
    String generateToken(String userId, List<String> roomIds, String examId, List<String> roles, List<String> streamTypes, OffsetDateTime windowStart, OffsetDateTime windowEnd);
}
