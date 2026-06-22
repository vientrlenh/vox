package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.UpdateSchoolStatusCommand;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdateSchoolStatusCommandMapper {

    public static UpdateSchoolStatusCommand fromRequest(UUID schoolId, boolean isActive) {
        return new UpdateSchoolStatusCommand(
                schoolId,
                isActive
        );
    }
}