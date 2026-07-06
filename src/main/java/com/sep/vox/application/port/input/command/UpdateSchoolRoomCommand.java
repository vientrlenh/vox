package com.sep.vox.application.port.input.command;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateSchoolRoomCommand(
        UUID id,
        String name,
        String description,
        Integer capacity
) {
}