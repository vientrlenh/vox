package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

public record UpdateSchoolRoomRequest(
        String name,
        String description
) {
}
