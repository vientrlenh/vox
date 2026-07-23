package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record GetExamRecordsQuery(
    UUID examSessionId, 
    String streamType
) {
    
}
