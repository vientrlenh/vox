package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;

public record SchoolUsersKey(
    UUID schoolId, 
    int page, 
    int size
) {
    
}
