package com.sep.vox.interfaces.graphql.dto.key;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record SchoolUserPageKey(
    UUID schoolId, 
    AppPageRequest pageRequest
) {
    
}
