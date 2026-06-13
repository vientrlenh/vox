package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ListSchoolUsersCommand(
    UUID schoolId,
    int page,
    int size
) {

}
