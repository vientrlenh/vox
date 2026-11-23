package com.sep.vox.application.port.input.query.dashboard;

import java.time.Instant;

public record ViewPlatformOperationalHealthQuery(
    Instant dateFrom, 
    Instant dateTo
) {
    
}
