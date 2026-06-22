package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.DeleteSchoolCommand;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeleteSchoolCommandMapper {
    public static DeleteSchoolCommand fromRequest(UUID schoolId) {
        return new DeleteSchoolCommand(schoolId);
    }
}
