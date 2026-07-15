package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppealTaskInfo(
    UUID appealId,
    String examName,
    String partLabel,
    OffsetDateTime deadline,
    String myStatus
) {
}
