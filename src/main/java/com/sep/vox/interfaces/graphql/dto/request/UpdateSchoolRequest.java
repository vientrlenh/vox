package com.sep.vox.interfaces.graphql.dto.request;


public record UpdateSchoolRequest(
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String domain,
        String address,
        Integer studentCount
) {}