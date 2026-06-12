package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record SchoolClassGradeKey(
    UUID schoolGradeId,
    UUID schoolId
) {
}
