package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.UpdateSystemRubricCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateSystemRubricRequest;

import java.util.UUID;

public class UpdateSystemRubricCommandMapper {
    public static UpdateSystemRubricCommand fromRequest(UUID rubricId, UpdateSystemRubricRequest request) {
        return new UpdateSystemRubricCommand(
                rubricId,
                request.name(),
                request.description()
        );
    }
}