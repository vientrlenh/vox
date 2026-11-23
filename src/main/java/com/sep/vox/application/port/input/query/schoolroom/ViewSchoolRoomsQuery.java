package com.sep.vox.application.port.input.query.schoolroom;

import java.util.UUID;



public record ViewSchoolRoomsQuery(
        UUID schoolId,
        int page, 
        int size
) {
}
