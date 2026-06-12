package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolClassUserCommand(UUID schoolId, UUID classId, UUID userId) {
}
