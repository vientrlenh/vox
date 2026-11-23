package com.sep.vox.application.port.input.command.schoolroom;

import java.util.UUID;

public record DeleteSchoolRoomCommand(
        UUID schoolId,
        UUID id) {
}