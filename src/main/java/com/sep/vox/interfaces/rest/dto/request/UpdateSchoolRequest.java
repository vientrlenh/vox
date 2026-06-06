package com.sep.vox.interfaces.rest.dto.request;


import java.util.UUID;

public record UpdateSchoolRequest(
        UUID id,
        String code,
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String domain,
        String address,
        Integer studentCount
) {}