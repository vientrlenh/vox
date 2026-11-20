package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** AI quality report; an empty {@code assessmentId} = the whole school. */
public record ViewAiQualityReportQuery(
    UUID assessmentId
) {
}
