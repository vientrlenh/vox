package com.sep.vox.application.port.input.command.schooluser;

import java.util.UUID;

public record DeleteSchoolUserCommand(
    UUID schoolId,
    UUID userId
) {

}
