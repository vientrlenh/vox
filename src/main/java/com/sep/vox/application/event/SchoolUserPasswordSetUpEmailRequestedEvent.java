package com.sep.vox.application.event;

import java.util.UUID;

public record SchoolUserPasswordSetUpEmailRequestedEvent(
    String to,
    String schoolUserName,
    String schoolName,
    UUID userId,
    String rawToken
) {
}