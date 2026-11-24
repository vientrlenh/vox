package com.sep.vox.application.port.input.command.school;

import java.util.UUID;

public record UpdateSchoolCommand(
        UUID id,
        String name,
        String description,
        boolean descriptionProvided,
        String contactPhone,
        String contactEmail,
        String domain,
        boolean domainProvided,
        String address,
        Integer studentCount
) {}