package com.sep.vox.application.port.input.query.schooluser;

import java.util.UUID;

public record ViewSchoolUserDetailsQuery(
    UUID schoolId,
    UUID userId
) {

}
