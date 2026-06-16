package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolClassUsersQuery(UUID schoolClassId, int page, int size) {
}
