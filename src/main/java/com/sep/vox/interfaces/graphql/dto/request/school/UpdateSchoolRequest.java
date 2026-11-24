package com.sep.vox.interfaces.graphql.dto.request.school;

import com.sep.vox.application.port.input.command.school.UpdateSchoolCommand;
import java.util.UUID;

public record UpdateSchoolRequest(
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String domain,
        String address,
        Integer studentCount
) {

    public static UpdateSchoolCommand toCommand(UUID id,UpdateSchoolRequest request) {
        if (request == null) return null;

        return new UpdateSchoolCommand(
                id,
                request.name(),
                request.description(),
                request.contactPhone(),
                request.contactEmail(),
                request.domain(),
                request.address(),
                request.studentCount()
        );
    }
}
