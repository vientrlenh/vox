package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewMyClassesQuery(
    UUID schoolId,
    String search,
    String status,
    int page,
    int size
) {

}
