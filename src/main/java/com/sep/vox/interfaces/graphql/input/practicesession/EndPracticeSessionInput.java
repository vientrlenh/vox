package com.sep.vox.interfaces.graphql.input.practicesession;

import java.util.UUID;

public record EndPracticeSessionInput(
        UUID sessionId,
        int helpRequestCount,
        int longPauseCount) {
}
