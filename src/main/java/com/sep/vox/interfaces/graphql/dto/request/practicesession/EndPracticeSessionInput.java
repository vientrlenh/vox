package com.sep.vox.interfaces.graphql.dto.request.practicesession;

import java.util.UUID;

public record EndPracticeSessionInput(
        UUID sessionId,
        int helpRequestCount,
        int longPauseCount) {
}
