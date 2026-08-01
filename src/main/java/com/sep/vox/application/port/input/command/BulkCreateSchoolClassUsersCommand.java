package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record BulkCreateSchoolClassUsersCommand(UUID schoolId, UUID classId, List<UUID> userIds) {

}
