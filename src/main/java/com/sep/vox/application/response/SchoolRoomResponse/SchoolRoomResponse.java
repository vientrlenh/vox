package com.sep.vox.application.response.SchoolRoomResponse;

import java.util.UUID;

public record SchoolRoomResponse(
        UUID id,
        UUID schoolId,
        String code,
        String name,
        String description,
        boolean isActive
) {
}