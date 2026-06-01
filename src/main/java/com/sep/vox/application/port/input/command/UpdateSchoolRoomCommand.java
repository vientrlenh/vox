package com.sep.vox.application.port.input.command;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateSchoolRoomCommand(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        boolean isActive
) {
}