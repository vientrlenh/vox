package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdateSchoolCommandMapper {

    public static UpdateSchoolCommand fromRequest(UUID schoolId, UpdateSchoolRequest request) {
        if (request == null) return null;

        return new UpdateSchoolCommand(
                schoolId,
                request.schoolCode(),
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