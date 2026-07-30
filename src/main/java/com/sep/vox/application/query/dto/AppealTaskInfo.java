package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppealTaskInfo(
    UUID appealId,
    String examName,
    List<String> partLabels,
    Instant deadline,
    String myStatus,
    boolean overdue
) {
}
