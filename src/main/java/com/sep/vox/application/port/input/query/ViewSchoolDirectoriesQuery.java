package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolDirectoriesQuery(
    UUID cursor, 
    int limit
) {
    
}
