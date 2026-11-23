package com.sep.vox.application.port.input.query.schoolclass;

import java.util.UUID;

public record ViewMyClassMembersQuery(
    UUID schoolClassId,
    String roleCode,
    String search,
    int page,
    int size
) {

}
