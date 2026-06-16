package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSchoolClassUserStatusCommand(UUID schoolId, UUID classId, UUID userId, boolean isActive) {
}
