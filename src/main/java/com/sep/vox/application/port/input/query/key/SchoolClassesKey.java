package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record SchoolClassesKey(
    UUID schoolId, 
    int page, 
    int size
) {
    
}
