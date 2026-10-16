package com.sep.vox.application.port.input.command;

public record  UpdateProfileCommand(
        String fullName,
        boolean fullNameProvided,
        String phone,
        boolean phoneProvided,
        String address,
        boolean addressProvided,
        String avatarUrl,
        boolean avatarUrlProvided
) {
}
