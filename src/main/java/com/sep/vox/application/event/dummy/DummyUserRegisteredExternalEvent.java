package com.sep.vox.application.event.dummy;

public record DummyUserRegisteredExternalEvent(
    String userId,
    String email,
    String fullName
) {
}
