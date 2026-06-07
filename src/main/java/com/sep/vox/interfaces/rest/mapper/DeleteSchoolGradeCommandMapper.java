package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.DeleteSchoolGradeCommand;

import java.util.UUID;

public class DeleteSchoolGradeCommandMapper {
    public static DeleteSchoolGradeCommand fromRequest(UUID id) {
        return new DeleteSchoolGradeCommand(id);
    }}
