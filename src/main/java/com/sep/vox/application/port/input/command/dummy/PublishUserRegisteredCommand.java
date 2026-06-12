package com.sep.vox.application.port.input.command.dummy;

public record PublishUserRegisteredCommand(
    String userId,
    String email,
    String fullName
) {}
