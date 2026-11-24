package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;

public record SchoolClassesKey(
    UUID schoolId, 
    int page, 
    int size
) {
    
}
