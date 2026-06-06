package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

public record UpdateSchoolRoomRequest(
        UUID id,
        String name,
        String description,
        Boolean isActive
) {
}
