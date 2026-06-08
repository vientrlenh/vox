package com.sep.vox.application.event.dummy;

import com.sep.vox.application.event.ExternalEventTopic;

@ExternalEventTopic("user-events")
public record DummyUserRegisteredExternalEvent(
    String userId,
    String email,
    String fullName
) {
}
