package com.sep.vox.application.port.input.query;

public record ViewSchoolsQuery(
    int page,
    int size,
    String search,
    Boolean isActive
) {

}
