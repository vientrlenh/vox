package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ChangeSchoolUserRoleCommand(
    UUID schoolId,
    UUID userId,
    String newRoleCode
) {

}
