package com.sep.vox.application.port.input.query;

import java.util.UUID;


import com.sep.vox.domain.common.PageRequest;

public record ViewSchoolRoomsQuery(
        UUID schoolId,
        PageRequest pageRequest
) {
}
