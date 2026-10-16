package com.sep.vox.application.port.input.query;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSchoolsQuery(
    AppPageRequest pageRequest,
    String search,
    Boolean isActive
) {

}
