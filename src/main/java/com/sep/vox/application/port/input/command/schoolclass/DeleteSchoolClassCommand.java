package com.sep.vox.application.port.input.command.schoolclass;

import java.util.UUID;

public record DeleteSchoolClassCommand(UUID schoolId, UUID id) {
}
