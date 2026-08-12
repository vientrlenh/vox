package com.sep.vox.application.event;

import java.util.UUID;

public record PracticeSessionEndedEvent(UUID studentId, UUID sessionId) {
}
