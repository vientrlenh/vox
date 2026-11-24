package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;

public record SchoolClassUsersKey(
    UUID schoolClassId, 
    int page, 
    int size
) {
}
