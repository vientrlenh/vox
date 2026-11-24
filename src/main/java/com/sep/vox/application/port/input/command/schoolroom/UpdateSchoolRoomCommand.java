package com.sep.vox.application.port.input.command.schoolroom;

import java.util.UUID;

public record UpdateSchoolRoomCommand(
        UUID id,
        String name,
        String description
) {
}
