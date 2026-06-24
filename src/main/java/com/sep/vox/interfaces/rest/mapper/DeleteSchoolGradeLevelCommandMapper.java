package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeLevelCommand;

import java.util.UUID;

public class DeleteSchoolGradeLevelCommandMapper {

    public static DeleteSchoolGradeLevelCommand fromRequest(UUID schoolId, UUID id) {
        return new DeleteSchoolGradeLevelCommand(
                schoolId,
                id);
    }
}