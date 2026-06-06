package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

public record UpdateSchoolRequest(
        UUID id,
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String domain,
        String address,
        Integer studentCount
) {}