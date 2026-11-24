package com.sep.vox.application.port.input.command.schoolgrade;

import java.util.UUID;

public record DeleteSchoolGradeCommand(
        UUID schoolId,
        UUID id) {
}
