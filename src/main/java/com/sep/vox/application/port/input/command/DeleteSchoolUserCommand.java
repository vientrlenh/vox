package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolUserCommand(
    UUID schoolId,
    UUID userId
) {

}
