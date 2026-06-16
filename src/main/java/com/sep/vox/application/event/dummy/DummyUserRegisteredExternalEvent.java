package com.sep.vox.application.event.dummy;

import com.sep.vox.application.event.ExternalEventTopic;

@ExternalEventTopic("user.registered.v1")
public record DummyUserRegisteredExternalEvent(
    String userId,
    String email,
    String fullName
) {
}
