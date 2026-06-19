package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolUserDetailsQuery(
    UUID schoolId,
    UUID userId
) {

}
