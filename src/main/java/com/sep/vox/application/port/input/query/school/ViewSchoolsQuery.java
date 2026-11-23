package com.sep.vox.application.port.input.query.school;

public record ViewSchoolsQuery(
    int page,
    int size,
    String search,
    Boolean isActive
) {

}
