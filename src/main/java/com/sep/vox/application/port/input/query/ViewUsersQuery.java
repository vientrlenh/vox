package com.sep.vox.application.port.input.query;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewUsersQuery(
    AppPageRequest pageRequest
) {
    
}
