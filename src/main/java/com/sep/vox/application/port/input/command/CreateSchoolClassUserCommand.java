package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSchoolClassUserCommand(UUID schoolId, UUID classId, UUID userId) {
}
