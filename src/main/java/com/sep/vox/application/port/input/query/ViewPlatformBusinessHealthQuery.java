package com.sep.vox.application.port.input.query;

import java.time.Instant;

public record ViewPlatformBusinessHealthQuery(
    Instant dateFrom, 
    Instant dateTo
) {
    
}
