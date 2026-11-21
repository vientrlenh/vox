package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSchoolRoomCommand(
        UUID id,
        String name,
        String description
) {
}
