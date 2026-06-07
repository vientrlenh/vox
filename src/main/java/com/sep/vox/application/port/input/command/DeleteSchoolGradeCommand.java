package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolGradeCommand(
        UUID schoolId,
        UUID id) {
}
