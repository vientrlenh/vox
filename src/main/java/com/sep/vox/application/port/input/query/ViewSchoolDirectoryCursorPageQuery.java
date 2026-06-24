package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolDirectoryCursorPageQuery(
    UUID cursor, 
    int limit
) {
    
}
