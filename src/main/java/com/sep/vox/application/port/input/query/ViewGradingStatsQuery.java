package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewGradingStatsQuery(
    UUID examId,
    UUID scheduleId
) {
}
