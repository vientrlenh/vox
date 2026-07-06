package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AddSchoolRoomCommand(
        UUID schoolId,
        String code,
        String name,
        String description
) { }