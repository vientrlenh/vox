package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.common.PageRequest;
import java.util.UUID;

public record ViewSchoolGradesQuery(
        UUID schoolId,
        PageRequest pageRequest
) {
}