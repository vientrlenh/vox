package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppealTaskInfo(
    UUID appealId,
    String examName,
    List<String> partLabels,
    OffsetDateTime deadline,
    String myStatus,
    boolean overdue
) {
}
