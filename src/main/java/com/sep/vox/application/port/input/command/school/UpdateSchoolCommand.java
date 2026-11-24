package com.sep.vox.application.port.input.command.school;

import java.util.UUID;

public record UpdateSchoolCommand(
        UUID id,
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String domain,
        String address,
        Integer studentCount
) {}