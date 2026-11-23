package com.sep.vox.application.port.input.query.schoolclassuser;

import java.util.UUID;

public record ViewSchoolClassUsersQuery(UUID schoolClassId, int page, int size) {
}
