package com.sep.vox.interfaces.graphql.dto.key;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record SchoolClassPageKey(
    UUID schoolId, 
    AppPageRequest pageRequest
) {
}
